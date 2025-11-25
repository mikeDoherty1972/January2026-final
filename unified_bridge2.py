#!/usr/bin/env python3
"""
# Unified Bridge: Modbus PLC and Security Sensor Data Synchronization
This script provides two main functions:
1. Modbus Bridge: Monitors Firestore for lights commands and controls PLC via Modbus
2. Security Sensors: Reads zone sensor files and uploads to Firestore with FCM notifications

Author: Copilot / Gemini (2025-11-14 update)
Date: 2025-11-15 
"""

import time
import socket
import struct
import os
import sys
import threading
from datetime import datetime

# Firestore imports
import firebase_admin
from firebase_admin import credentials, firestore

try:
    from fcm_v1_send import send_fcm_v1_notification, fetch_fcm_tokens
    FCM_AVAILABLE = True
except ImportError:
    # Use log_print for this warning
    print("Warning: fcm_v1_send module not found. FCM notifications will be disabled.")
    FCM_AVAILABLE = False

# ==================== CONFIGURATION ====================
# Firebase Configuration
SERVICE_ACCOUNT_FILE = r"C:\website\firebase\security-33809-firebase-adminsdk-fbsvc-5bbc47b722.json"
FIRESTORE_PROJECT = "security-33809"

# Sensor Files Path
SENSOR_FILE_PATH = r"C:\Users\miked\My Drive\python\current\sensors"

# Firestore Collections/Documents for Modbus Bridge
FS_COLLECTION = "Flights_commands"
FS_DOC = "current_command"

# PLC / Modbus Configuration
PLC_HOST = "192.168.8.5"
PLC_PORT = 8080
UNIT_ID = 1
ADDRESS_IS_1_BASED = True

# Coils
COIL_DOC_2070 = 2070    # Command coil
COIL_DOC_1298 = 1298    # Confirmation coil

# Timing Configuration
MODBUS_POLL_INTERVAL = 1.0      # seconds to poll Firestore for new commands (UNUSED in listener mode)
SECURITY_POLL_INTERVAL = 15.0   # seconds to poll security sensors
SECURITY_HEARTBEAT_INTERVAL = 300.0  # seconds between heartbeat writes (5 minutes)
CONNECT_TIMEOUT = 4.0
RECV_TIMEOUT = 3.0
DRAIN_SEC = 0.12
CONFIRM_RETRY_COUNT = 6
CONFIRM_RETRY_DELAY = 0.5

# Security Zone Sensor Files
ZONE_SENSOR_FILES = {
    'garage_motion': "garage_motion.txt",
    'garage_sensor': "garage_sensor.txt",
    'garage_side_motion': "garage_side_motion.txt",
    'garage_side_sensor': "garage_side_sensor.txt",
    'south_motion': "south_motion.txt",
    'south_sensor': "south_sensor.txt",
    'back_motion': "back_motion.txt",
    'back_sensor': "back_sensor.txt",
    'north_motion': "north_motion.txt",
    'north_sensor': "north_sensor.txt",
    'front_motion': "front_motion.txt",
    'front_sensor': "front_sensor.txt"
}

# Zone Definitions for Alerts
ZONES = [
    {"name": "Garage", "sensors": ["garage_motion", "garage_sensor"]},
    {"name": "Garage Side", "sensors": ["garage_side_motion", "garage_side_sensor"]},
    {"name": "South", "sensors": ["south_motion", "south_sensor"]},
    {"name": "Back", "sensors": ["back_motion", "back_sensor"]},
    {"name": "North", "sensors": ["north_motion", "north_sensor"]},
    {"name": "Front", "sensors": ["front_motion", "front_sensor"]}
]

# ==================== UTILITY FUNCTIONS ====================
def now_iso():
    """Returns current UTC time in ISO format."""
    return datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

def terminal_timestamp():
    """Returns current local time in a concise format for terminal output."""
    return datetime.now().strftime("[%Y-%m-%d %H:%M:%S]")

def log_print(*args, **kwargs):
    """Prints output prefixed with a local timestamp."""
    timestamp = terminal_timestamp()
    # Join the arguments passed to log_print into a single string
    message = " ".join(map(str, args))
    # Print the timestamp followed by the message
    print(f"{timestamp} {message}", **kwargs)

def doc_to_modbus(doc):
    """Converts document address to Modbus address."""
    return doc - 1 if ADDRESS_IS_1_BASED else doc
# ==================== END UTILITY FUNCTIONS ====================


# ==================== INITIALIZATION ====================
os.system("title Unified Bridge") # ✅ Updated to simpler title

# Initialize Firebase
try:
    cred = credentials.Certificate(SERVICE_ACCOUNT_FILE)
    firebase_admin.initialize_app(cred)
    # Using log_print for status messages
    log_print("✓ Firebase initialized successfully")
except Exception as e:
    log_print(f"✗ Error initializing Firebase: {e}")
    sys.exit(1)

db = firestore.client()

# ==================== MODBUS FUNCTIONS ====================
def build_mbap_request(func, tid, unit, addr, qty_or_value):
    """Builds Modbus MBAP request packet."""
    pdu = struct.pack(">BHH", func, addr & 0xFFFF, qty_or_value & 0xFFFF)
    length = 1 + len(pdu)
    return struct.pack(">HHHB", tid & 0xFFFF, 0, length & 0xFFFF, unit & 0xFF) + pdu

def recv_mbap_response(sock, timeout):
    """Receives Modbus MBAP response."""
    sock.settimeout(timeout)
    try:
        hdr = sock.recv(7)
    except socket.timeout:
        return None, "no header (timeout)"
    except Exception as e:
        return None, f"recv header err: {e}"
    if not hdr or len(hdr) < 7:
        return None, "short header"
    try:
        resp_tid, proto, length, unit = struct.unpack(">HHHB", hdr)
    except Exception as e:
        return None, f"bad header unpack: {e}"
    toread = max(0, length - 1)
    pdu = b""
    deadline = time.time() + timeout
    while len(pdu) < toread and time.time() < deadline:
        try:
            chunk = sock.recv(toread - len(pdu))
        except socket.timeout:
            break
        except Exception:
            break
        if not chunk:
            break
        pdu += chunk
    return (hdr, pdu), None

def modbus_write_coil(host, port, unit, addr, value, timeout):
    """Writes a single coil via Modbus (Function Code 5)."""
    tid = int(time.time() * 1000) & 0xFFFF
    coil_value = 0xFF00 if value else 0x0000
    req = build_mbap_request(0x05, tid, unit, addr, coil_value)
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(timeout)
            sock.connect((host, port))
            sock.sendall(req)
            time.sleep(DRAIN_SEC)
            resp, err = recv_mbap_response(sock, RECV_TIMEOUT)
            if err:
                return False, err
            return True, "OK"
    except Exception as e:
        return False, str(e)

def modbus_read_coil(host, port, unit, addr, timeout):
    """Reads a single coil via Modbus (Function Code 1)."""
    tid = int(time.time() * 1000) & 0xFFFF
    req = build_mbap_request(0x01, tid, unit, addr, 1)
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(timeout)
            sock.connect((host, port))
            sock.sendall(req)
            time.sleep(DRAIN_SEC)
            resp, err = recv_mbap_response(sock, RECV_TIMEOUT)
            if err:
                return None, err
            hdr, pdu = resp
            if len(pdu) < 2:
                return None, "short pdu"
            fc = pdu[0]
            if fc != 0x01:
                return None, f"unexpected fc={fc}"
            byte_count = pdu[1]
            if len(pdu) < 2 + byte_count:
                return None, "incomplete data"
            data = pdu[2:2+byte_count]
            if len(data) == 0:
                return None, "no data bytes"
            coil_state = bool(data[0] & 0x01)
            return coil_state, None
    except Exception as e:
        return None, str(e)

# ==================== MODBUS BRIDGE THREAD ====================
def modbus_bridge_loop():
    """
    Main execution loop for Modbus bridge.
    
    ACTION: This has been converted from a continuous polling loop (.get() every 1s)
            to a real-time on_snapshot listener to drastically reduce Firestore reads.
            Reads will only occur when the 'current_command' document is updated.
    """
    log_print("▶ Starting Modbus Bridge thread (Listener Mode)...")
    cmd_ref = db.collection(FS_COLLECTION).document(FS_DOC)
    status_ref = db.collection("scada_controls").document("lights_status")
    
    # [mutable] Use a list to store the last timestamp, allowing the closure to modify it.
    last_cmd_ts_container = [None]
    
    # ------------------ ON_SNAPSHOT CALLBACK (Closure) ------------------
    def on_command_snapshot(doc_snapshot, changes, read_time):
        """Callback function executed by the Firestore SDK when the document changes."""
        try:
            # Check for initial load or errors
            if doc_snapshot and doc_snapshot[0].exists:
                doc = doc_snapshot[0] # Get the single document snapshot
            else:
                log_print("[Modbus] Listener received no document or error.")
                return

            data = doc.to_dict()
            cmd = data.get("command", "").lower()
            cmd_ts = data.get("command_ts")
            
            # Check if this is a new command based on the timestamp
            if cmd_ts == last_cmd_ts_container[0] or not cmd:
                log_print("[Modbus] Command is not new or is empty. Skipping processing.")
                return
            
            # Set the new timestamp immediately before processing
            last_cmd_ts_container[0] = cmd_ts
            
            # Process command
            if cmd not in ["on", "off"]:
                log_print(f"[Modbus] Unknown command: {cmd}. Ignoring.")
                return
            
            desired_state = (cmd == "on")
            log_print(f"[Modbus] New command received: {cmd.upper()} at {now_iso()}")
            
            # Write to command coil
            cmd_addr = doc_to_modbus(COIL_DOC_2070)
            success, msg = modbus_write_coil(PLC_HOST, PLC_PORT, UNIT_ID, cmd_addr, desired_state, CONNECT_TIMEOUT)
            
            if not success:
                log_print(f"[Modbus] Write failed: {msg}")
                status_ref.set({
                    "error": msg,
                    "timestamp": firestore.SERVER_TIMESTAMP
                }, merge=True)
                return
            
            log_print(f"[Modbus] Write successful, confirming...")
            
            # Confirm by reading confirmation coil multiple times
            confirm_addr = doc_to_modbus(COIL_DOC_1298)
            confirmed = False
            attempts = []
            
            for attempt in range(CONFIRM_RETRY_COUNT):
                time.sleep(CONFIRM_RETRY_DELAY)
                state, err = modbus_read_coil(PLC_HOST, PLC_PORT, UNIT_ID, confirm_addr, CONNECT_TIMEOUT)
                attempts.append({"attempt": attempt + 1, "state": state, "error": err})
                
                if err:
                    log_print(f"[Modbus] Confirm attempt {attempt + 1}: ERROR - {err}")
                else:
                    log_print(f"[Modbus] Confirm attempt {attempt + 1}: {state}")
                    if state == desired_state:
                        confirmed = True
                        break
            
            # Update status document
            status_data = {
                "coil_1298_state": confirmed,
                "coil_1298_state_bool": confirmed,
                "command": cmd,
                "confirmed": confirmed,
                "attempts": len(attempts),
                "timestamp": firestore.SERVER_TIMESTAMP,
                "bridge_version": "unified_v1"
            }
            status_ref.set(status_data, merge=True)
            
            result = "CONFIRMED" if confirmed else "NOT CONFIRMED"
            log_print(f"[Modbus] Result: {result} after {len(attempts)} attempts")
            print() # Add newline for spacing

        except Exception as e:
            log_print(f"[Modbus Listener] Unhandled error during processing: {e}")
    # ------------------ END ON_SNAPSHOT CALLBACK ------------------

    # Start the real-time listener (non-blocking)
    try:
        cmd_watch = cmd_ref.on_snapshot(on_command_snapshot)
    except Exception as e:
        log_print(f"✗ Failed to attach Modbus listener: {e}")
        return

    # Keep the thread running for the listener to function.
    try:
        while True:
            # Sleep prevents the thread from consuming CPU while the listener waits for events.
            time.sleep(999999) 
    except KeyboardInterrupt:
        # Listener must be detached on shutdown
        if 'cmd_watch' in locals() and cmd_watch:
            cmd_watch.unsubscribe()
        log_print("[Modbus] Listener thread shutting down.")
        
# ==================== SECURITY SENSORS FUNCTIONS ====================
def read_sensor_status(filename):
    """Reads sensor file, strips all whitespace, and returns status (0 or 1)."""
    full_path = os.path.join(SENSOR_FILE_PATH, filename)
    try:
        with open(full_path, 'r') as f:
            # Read ALL content, then aggressively strip all leading/trailing whitespace.
            # This handles blank lines and trailing newlines better than readline().
            content = f.read().strip() 
            
            # Validate that the stripped content is exactly one character ('0' or '1')
            if len(content) != 1 or content not in ['0', '1']:
                log_print(f"[Security] Error: Invalid data (not 0 or 1) in {filename} - found '{content}'")
                return -1 # Return error status
                
            return int(content)
            
    except FileNotFoundError:
        log_print(f"[Security] Error: File not found - {full_path}")
        return -1
    except ValueError:
        # Catch if int() fails unexpectedly (e.g., non-numeric data that passed the length check)
        log_print(f"[Security] Error: Invalid data in {filename}")
        return -1
    except Exception as e:
        log_print(f"[Security] Error reading {filename}: {e}")
        return -1

def send_security_data(previous_state, force_write=False):
    """
    Reads all sensor files and uploads to Firestore only if state changed.
    
    Args:
        previous_state: Dictionary of previous sensor states
        force_write: If True, write to Firestore regardless of changes (for heartbeat)
    
    Returns:
        Tuple of (current_state dict, write_occurred bool)
    """
    data = {'timestamp': firestore.SERVER_TIMESTAMP}
    errors = False
    
    for sensor_name, filename in ZONE_SENSOR_FILES.items():
        status = read_sensor_status(filename)
        if status == -1:
            errors = True
        data[sensor_name] = status
    
    if errors:
        log_print("[Security] Skipping upload due to read errors")
        return previous_state, False
    
    # Check if any sensor state has changed
    state_changed = False
    if previous_state is None:
        state_changed = True
        log_print("[Security] Initial state detected")
    else:
        for sensor_name in ZONE_SENSOR_FILES.keys():
            if data.get(sensor_name) != previous_state.get(sensor_name):
                old_val = previous_state.get(sensor_name, '?')
                new_val = data.get(sensor_name, '?')
                log_print(f"[Security] State change: {sensor_name} {old_val}→{new_val}")
                state_changed = True
    
    # Only write to Firestore if state changed or forced (heartbeat)
    if state_changed or force_write:
        # Check for active zones and send FCM notifications (only on state change, not heartbeat)
        if state_changed and FCM_AVAILABLE:
            for zone in ZONES:
                s1, s2 = zone["sensors"]
                if data.get(s1) == 1 and data.get(s2) == 1:
                    try:
                        send_fcm_v1_notification(
                            f"{zone['name']} Alert",
                            f"Both sensors in zone {zone['name']} are ACTIVE!"
                        )
                        log_print(f"[Security] FCM sent for {zone['name']} zone")
                    except Exception as e:
                        log_print(f"[Security] FCM error for {zone['name']}: {e}")
        
        # Upload to Firestore
        try:
            db.collection('security sensors').document('live_status').set(data)
            active_sensors = [k for k, v in data.items() if k != 'timestamp' and v == 1]
            
            if force_write:
                log_print(f"[Security] Heartbeat write - {len(active_sensors)} sensors active")
            elif active_sensors:
                log_print(f"[Security] Status uploaded - Active: {', '.join(active_sensors)}")
            else:
                log_print(f"[Security] Status uploaded - All clear")
            
            # Return current state without timestamp for comparison
            current_state = {k: v for k, v in data.items() if k != 'timestamp'}
            return current_state, True
        except Exception as e:
            log_print(f"[Security] Upload error: {e}")
            return previous_state, False
    else:
        # No change, no write needed
        current_state = {k: v for k, v in data.items() if k != 'timestamp'}
        return current_state, False

def security_sensors_loop():
    """Main loop for security sensors - reads files and uploads to Firestore only on state changes."""
    log_print("▶ Starting Security Sensors thread...")
    log_print(f"[Security] Change-of-state detection enabled")
    log_print(f"[Security] Heartbeat interval: {SECURITY_HEARTBEAT_INTERVAL}s ({SECURITY_HEARTBEAT_INTERVAL/60:.1f} min)")
    
    previous_state = None
    last_heartbeat_time = time.time()
    
    while True:
        try:
            # Check if heartbeat write is due
            current_time = time.time()
            time_since_heartbeat = current_time - last_heartbeat_time
            force_write = time_since_heartbeat >= SECURITY_HEARTBEAT_INTERVAL
            
            # Read sensors and conditionally write
            previous_state, write_occurred = send_security_data(previous_state, force_write)
            
            # Update heartbeat timer if we wrote
            if write_occurred:
                last_heartbeat_time = current_time
            
            time.sleep(SECURITY_POLL_INTERVAL)
        except KeyboardInterrupt:
            log_print("[Security] Sensors stopped by user")
            break
        except Exception as e:
            log_print(f"[Security] Error: {e}")
            time.sleep(SECURITY_POLL_INTERVAL)

# ==================== MAIN ====================
def main():
    """Main entry point - starts both threads."""
    print("=" * 60) # Structural element, not informational, leave as is
    log_print("UNIFIED BRIDGE - Modbus + Security Sensors")
    print("=" * 60) # Structural element, not informational, leave as is
    log_print(f"Firebase Project: {FIRESTORE_PROJECT}")
    log_print(f"PLC: {PLC_HOST}:{PLC_PORT}")
    log_print(f"Sensor Path: {SENSOR_FILE_PATH}")
    log_print(f"Modbus Mode: Real-time Listener (Minimizing Reads)") 
    log_print(f"Security Poll: {SECURITY_POLL_INTERVAL}s")
    print("=" * 60) # Structural element, not informational, leave as is
    print()
    
    # Create and start threads
    modbus_thread = threading.Thread(target=modbus_bridge_loop, daemon=True, name="ModbusBridge")
    security_thread = threading.Thread(target=security_sensors_loop, daemon=True, name="SecuritySensors")
    
    modbus_thread.start()
    security_thread.start()
    
    log_print("✓ Both threads started successfully")
    log_print("Press Ctrl+C to stop...")
    print() # Add newline for spacing
    
    # Keep main thread alive
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n" + "=" * 60) # Structural element, not informational, leave as is
        log_print("Shutting down unified bridge...")
        print("=" * 60) # Structural element, not informational, leave as is
        sys.exit(0)

if __name__ == "__main__":
    main()
