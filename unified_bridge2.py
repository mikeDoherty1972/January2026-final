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
import re

# Firestore imports
import firebase_admin
from firebase_admin import credentials, firestore

try:
    from fcm_v1_send import send_fcm_v1_notification
    FCM_AVAILABLE = True
except ImportError:
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
LIGHTS_STATUS_DOC = ("scada_controls", "lights_status")

# PLC / Modbus Configuration
PLC_HOST = "192.168.8.5"
PLC_PORT = 8080
UNIT_ID = 1

# Addresses (1-based in your PLC notes)
PLC_GARAGE_SIDE_ADDR = 2129
COIL_DOC_2070 = 2070  # lights command coil
COIL_DOC_1298 = 1298  # confirmation coil
ADDRESS_IS_1_BASED = True

# Timing Configuration
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
    'front_sensor': "front_sensor.txt",
    'door_motion': "door_motion.txt",
    'door_sensor': "door_sensor.txt"
}

# Zone Definitions for Alerts
ZONES = [
    {"name": "Garage", "sensors": ["garage_motion", "garage_sensor"]},
    {"name": "Garage Side", "sensors": ["garage_side_motion", "garage_side_sensor"]},
    {"name": "South", "sensors": ["south_motion", "south_sensor"]},
    {"name": "Back", "sensors": ["back_motion", "back_sensor"]},
    {"name": "North", "sensors": ["north_motion", "north_sensor"]},
    {"name": "Front", "sensors": ["front_motion", "front_sensor"]},
    {"name": "Door", "sensors": ["door_motion", "door_sensor"]}
]

# ==================== UTILITY FUNCTIONS ====================
def now_iso():
    """Returns current UTC time in ISO format."""
    return datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

def terminal_timestamp():
    """Returns current local time in a concise format for terminal output."""
    return datetime.now().strftime("[%Y-%m-%d %H:%M:%S]")

def log_print(*args):
    """Prints output prefixed with a local timestamp."""
    timestamp = terminal_timestamp()
    # Join the arguments passed to log_print into a single string
    message = " ".join(map(str, args))
    # Print the timestamp followed by the message
    print(f"{timestamp} {message}")

def doc_to_modbus(addr):
    """Converts document address to Modbus address."""
    return addr - 1 if ADDRESS_IS_1_BASED else addr

# ==================== MODBUS helpers ====================
# Minimal helpers based on older working bridge

def _mbap_packet(func: int, tid: int, unit: int, addr: int, qty_or_val: int) -> bytes:
    pdu = struct.pack(">BHH", func & 0xFF, addr & 0xFFFF, qty_or_val & 0xFFFF)
    length = 1 + len(pdu)
    return struct.pack(">HHHB", tid & 0xFFFF, 0, length & 0xFFFF, unit & 0xFF) + pdu


def _recv_mbap(sock, timeout):
    sock.settimeout(timeout)
    try:
        hdr = sock.recv(7)
    except socket.timeout:
        return None, "timeout hdr"
    except Exception as e:
        return None, f"recv hdr err: {e}"
    if not hdr or len(hdr) < 7:
        return None, "short hdr"
    try:
        resp_tid, proto, length, unit = struct.unpack(">HHHB", hdr)
    except Exception as e:
        return None, f"bad hdr unpack: {e}"
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


def modbus_write_coil(addr: int, value: bool):
    tid = int(time.time() * 1000) & 0xFFFF
    coil_value = 0xFF00 if value else 0x0000
    req = _mbap_packet(0x05, tid, UNIT_ID, addr, coil_value)
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(CONNECT_TIMEOUT)
            s.connect((PLC_HOST, PLC_PORT))
            s.sendall(req)
            time.sleep(DRAIN_SEC)
            resp, err = _recv_mbap(s, RECV_TIMEOUT)
            if err:
                return False, err
            return True, "OK"
    except Exception as e:
        return False, str(e)


def modbus_read_coil(addr: int):
    tid = int(time.time() * 1000) & 0xFFFF
    req = _mbap_packet(0x01, tid, UNIT_ID, addr, 1)
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(CONNECT_TIMEOUT)
            s.connect((PLC_HOST, PLC_PORT))
            s.sendall(req)
            time.sleep(DRAIN_SEC)
            resp, err = _recv_mbap(s, RECV_TIMEOUT)
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
            data = pdu[2:2 + byte_count]
            if not data:
                return None, "no data"
            return bool(data[0] & 0x01), None
    except Exception as e:
        return None, str(e)


def modbus_read_discrete_input(addr: int):
    tid = int(time.time() * 1000) & 0xFFFF
    req = _mbap_packet(0x02, tid, UNIT_ID, addr, 1)
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(CONNECT_TIMEOUT)
            s.connect((PLC_HOST, PLC_PORT))
            s.sendall(req)
            time.sleep(DRAIN_SEC)
            resp, err = _recv_mbap(s, RECV_TIMEOUT)
            if err:
                return None, err
            hdr, pdu = resp
            if len(pdu) < 2:
                return None, "short pdu"
            fc = pdu[0]
            if fc == (0x02 | 0x80):
                return None, f"Modbus exception {pdu[1]}"
            if fc != 0x02:
                return None, f"unexpected fc={fc}"
            byte_count = pdu[1]
            if len(pdu) < 2 + byte_count:
                return None, "incomplete"
            data = pdu[2:2 + byte_count]
            if not data:
                return None, "no data"
            return int(bool(data[0] & 0x01)), None
    except Exception as e:
        return None, str(e)

# ==================== SECURITY LOOP (preserved) ====================
# ==================== UTILITIES ====================
last_file_values = {}

def read_sensor_status(filename, use_last_on_empty=False):
    full_path = os.path.join(SENSOR_FILE_PATH, filename)
    if not os.path.exists(full_path):
        return -1
    # Try up to 3 reads to avoid race conditions with writer
    attempts = 3
    raw = None
    cleaned = None
    for i in range(attempts):
        try:
            with open(full_path, 'r', encoding='utf-8', errors='ignore') as f:
                raw = f.readline()
            # Normalize: strip spaces, tabs and common non-digit noise
            if raw is None:
                raw = ''
            # Keep original for logging but build a cleaned token of digits only
            cleaned = re.sub(r'[^0-9]', '', raw.strip())
            if cleaned in ('0', '1'):
                val = int(cleaned)
                # Cache last good value
                last_file_values[filename] = val
                return val
            # If empty and we're allowed to use last cached value, try next loop or fallback
            if (cleaned == '' or cleaned is None):
                # short backoff, let writer finish
                time.sleep(0.1)
                continue
            # If it contains other digits like '10', collapse to last digit (defensive)
            if len(cleaned) > 1 and all(ch in '01' for ch in cleaned):
                val = int(cleaned[-1])
                last_file_values[filename] = val
                return val
        except Exception:
            time.sleep(0.1)
            continue
    # After retries, decide fallback
    if use_last_on_empty and filename in last_file_values:
        log_print(f"[Security] Warning: using cached value for {filename} due to empty/invalid read. Raw='{(raw or '').replace('\n','\\n').replace('\r','\\r')}'")
        return last_file_values[filename]
    # Log detailed invalid data for diagnostics
    log_print(f"[Security] Error: Invalid data (not 0 or 1) in {filename} - found '{(raw or '').strip()}'")
    return -1

def get_plc_sensor_status():
    modbus_addr = doc_to_modbus(PLC_GARAGE_SIDE_ADDR)
    status, err = modbus_read_discrete_input(modbus_addr)
    if err:
        log_print("[Security] PLC OFFLINE -", err)
        return -1
    return status

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
    
    # 1. Read FILE-BASED sensors (garage_side_sensor handled separately below)
    for sensor_name, filename in ZONE_SENSOR_FILES.items():
        # For garage_side_sensor.txt we allow cached fallback on empty
        use_last = (filename.lower().startswith('garage_side_sensor'))
        status = read_sensor_status(filename, use_last_on_empty=use_last)
        if status == -1:
            errors = True
        data[sensor_name] = status

    # 2. PLC-BASED sensor (if present in config)
    # If your bridge reads garage_side_sensor via PLC, keep this section. Otherwise it will simply overwrite the file value with PLC.
    if 'garage_side_sensor' in data:
        plc_val = get_plc_sensor_status()
        if plc_val != -1:
            data['garage_side_sensor'] = plc_val
        else:
            errors = True

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

# ==================== MODBUS BRIDGE LISTENER ====================

def modbus_bridge_loop(db):
    log_print("▶ Modbus Bridge: attaching Firestore listener")
    cmd_ref = db.collection(FS_COLLECTION).document(FS_DOC)
    status_ref = db.collection(LIGHTS_STATUS_DOC[0]).document(LIGHTS_STATUS_DOC[1])

    last_cmd_ts = [None]

    def on_command_snapshot(doc_snapshot, changes, read_time):
        try:
            if not doc_snapshot or not doc_snapshot[0].exists:
                log_print("[Modbus] No current_command doc")
                return
            data = doc_snapshot[0].to_dict() or {}
            cmd = (data.get("command") or "").lower()
            cmd_ts = data.get("command_ts")
            nonce = data.get("nonce")  # optional uniqueness token from app
            desired = (cmd == "on")
            # Use a richer signature so toggles with same ts are still processed
            current_sig = (cmd, desired, nonce, cmd_ts)
            if not cmd:
                return
            if last_cmd_ts[0] == current_sig:
                # same command + desired + nonce + ts => ignore
                log_print("[Modbus] duplicate command signature; ignoring")
                return
            last_cmd_ts[0] = current_sig
            log_print(f"[Modbus] New command: {cmd} (ts={cmd_ts}, nonce={nonce})")

            ok, msg = modbus_write_coil(doc_to_modbus(COIL_DOC_2070), desired)
            if not ok:
                log_print(f"[Modbus] Write failed: {msg}")
                status_ref.set({
                    "error": msg,
                    "timestamp": firestore.SERVER_TIMESTAMP,
                    "bridge_version": "unified_v6",
                }, merge=True)
                return
            log_print("[Modbus] Write OK, confirming coil 1298...")

            confirmed = False
            for i in range(CONFIRM_RETRY_COUNT):
                time.sleep(CONFIRM_RETRY_DELAY)
                state, err = modbus_read_coil(doc_to_modbus(COIL_DOC_1298))
                if err:
                    log_print(f"[Modbus] Confirm attempt {i+1}: err={err}")
                else:
                    log_print(f"[Modbus] Confirm attempt {i+1}: state={state}")
                    if state == desired:
                        confirmed = True
                        break

            status_ref.set({
                "command": cmd,
                "desired": desired,
                "command_ts": cmd_ts,
                "nonce": nonce,
                "coil_1298_state": confirmed,
                "attempts": CONFIRM_RETRY_COUNT,
                "timestamp": firestore.SERVER_TIMESTAMP,
                "bridge_version": "unified_v6",
            }, merge=True)
            log_print("[Modbus] Result:", "CONFIRMED" if confirmed else "NOT CONFIRMED")
        except Exception as e:
            log_print(f"[Modbus] Listener error: {e}")

    try:
        watch = cmd_ref.on_snapshot(on_command_snapshot)
        log_print("[Modbus] Listener active")
        while True:
            time.sleep(999999)
    except KeyboardInterrupt:
        try:
            watch.unsubscribe()
        except Exception:
            pass
        log_print("[Modbus] Listener stopped")
    except Exception as e:
        log_print(f"[Modbus] Failed to attach listener: {e}")

# ==================== STARTUP ====================
if __name__ == "__main__":
    os.system("title Unified Bridge V6")
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_FILE)
        firebase_admin.initialize_app(cred)
        db = firestore.client()
        log_print("✓ Firebase Connected")

        # Start threads: preserved security loop + new listener bridge
        t_sec = threading.Thread(target=security_sensors_loop, daemon=True, name="SecuritySensors")
        t_sec.start()
        t_mod = threading.Thread(target=modbus_bridge_loop, args=(db,), daemon=True, name="ModbusBridge")
        t_mod.start()

        # Add or update configuration and listener pieces
        try:
            FIRESTORE_PROJECT
        except NameError:
            FIRESTORE_PROJECT = "security-33809"

        # Lights bridge Firestore paths (align with existing usage)
        FS_COLLECTION = "Flights_commands"
        FS_DOC = "current_command"

        # PLC / coil addresses for lights bridge (keep consistent with older working script)
        COIL_DOC_2070 = 2070  # command coil
        COIL_DOC_1298 = 1298  # confirmation coil

        # If ADDRESS_IS_1_BASED is not defined in this script, default to True
        try:
            ADDRESS_IS_1_BASED
        except NameError:
            ADDRESS_IS_1_BASED = True

        # Helper: convert doc address to Modbus
        def _doc_to_modbus(addr: int) -> int:
            return addr - 1 if ADDRESS_IS_1_BASED else addr

        # Ensure door sensors are present in ZONE_SENSOR_FILES (idempotent)
        try:
            ZONE_SENSOR_FILES
            if 'door_motion' not in ZONE_SENSOR_FILES:
                ZONE_SENSOR_FILES['door_motion'] = "door_motion.txt"
            if 'door_sensor' not in ZONE_SENSOR_FILES:
                ZONE_SENSOR_FILES['door_sensor'] = "door_sensor.txt"
        except NameError:
            # If not defined earlier, create with the required door entries
            ZONE_SENSOR_FILES = {
                'garage_motion': "garage_motion.txt",
                'garage_sensor': "garage_sensor.txt",
                'garage_side_motion': "garage_side_motion.txt",
                'south_motion': "south_motion.txt",
                'south_sensor': "south_sensor.txt",
                'back_motion': "back_motion.txt",
                'back_sensor': "back_sensor.txt",
                'north_motion': "north_motion.txt",
                'north_sensor': "north_sensor.txt",
                'front_motion': "front_motion.txt",
                'front_sensor': "front_sensor.txt",
                'door_motion': "door_motion.txt",
                'door_sensor': "door_sensor.txt",
            }

        # Ensure ZONES include Door zone
        try:
            ZONES
            # Only append Door zone if not already present
            if not any(z.get('name') == 'Door' for z in ZONES):
                ZONES.append({"name": "Door", "sensors": ["door_motion", "door_sensor"]})
        except NameError:
            ZONES = [
                {"name": "Garage", "sensors": ["garage_motion", "garage_sensor"]},
                {"name": "Garage Side", "sensors": ["garage_side_motion", "garage_side_sensor"]},
                {"name": "South", "sensors": ["south_motion", "south_sensor"]},
                {"name": "Back", "sensors": ["back_motion", "back_sensor"]},
                {"name": "North", "sensors": ["north_motion", "north_sensor"]},
                {"name": "Front", "sensors": ["front_motion", "front_sensor"]},
                {"name": "Door", "sensors": ["door_motion", "door_sensor"]},
            ]

        # Lightweight Modbus helpers (reuse if already defined)
        try:
            modbus_write_coil
        except NameError:
            import socket, struct, time
            DRAIN_SEC = 0.12
            RECV_TIMEOUT = 3.0
            CONNECT_TIMEOUT = 4.0
            def _build_mbap(func, tid, unit, addr, qty_or_value):
                pdu = struct.pack(">BHH", func, addr & 0xFFFF, qty_or_value & 0xFFFF)
                length = 1 + len(pdu)
                return struct.pack(">HHHB", tid & 0xFFFF, 0, length & 0xFFFF, unit & 0xFF) + pdu
            def _recv_mbap(sock, timeout):
                sock.settimeout(timeout)
                hdr = sock.recv(7)
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
                    chunk = sock.recv(toread - len(pdu))
                    if not chunk:
                        break
                    pdu += chunk
                return (hdr, pdu), None
            def modbus_write_coil(host, port, unit, addr, value, timeout):
                tid = int(time.time() * 1000) & 0xFFFF
                coil_val = 0xFF00 if value else 0x0000
                req = _build_mbap(0x05, tid, unit, addr, coil_val)
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                        s.settimeout(timeout); s.connect((host, port)); s.sendall(req)
                        time.sleep(DRAIN_SEC)
                        resp, err = _recv_mbap(s, RECV_TIMEOUT)
                        if err:
                            return False, err
                        return True, "OK"
                except Exception as e:
                    return False, str(e)
            def modbus_read_coil(host, port, unit, addr, timeout):
                tid = int(time.time() * 1000) & 0xFFFF
                req = _build_mbap(0x01, tid, unit, addr, 1)
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                        s.settimeout(timeout); s.connect((host, port)); s.sendall(req)
                        time.sleep(DRAIN_SEC)
                        resp, err = _recv_mbap(s, RECV_TIMEOUT)
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
                        if not data:
                            return None, "no data bytes"
                        return bool(data[0] & 0x01), None
                except Exception as e:
                    return None, str(e)

        # Firestore listener for lights commands
        def start_modbus_bridge_listener(db, PLC_HOST, PLC_PORT, UNIT_ID):
            """Attach on_snapshot listener to Flights_commands/current_command and act on on/off.
            Writes detailed status to scada_controls/lights_status and logs clearly.
            """
            try:
                status_ref = db.collection("scada_controls").document("lights_status")
                cmd_ref = db.collection(FS_COLLECTION).document(FS_DOC)
                # Store last signature to avoid duplicate processing
                last_seen_sig = [None]
                def _log(*args):
                    try:
                        log_print(*args)
                    except Exception:
                        print(*args)
                def on_snapshot(doc_snapshot, changes, read_time):
                    if not doc_snapshot or not doc_snapshot[0].exists:
                        _log("[Modbus] Listener: no document snapshot")
                        return
                    data = doc_snapshot[0].to_dict() or {}
                    cmd = str(data.get("command", "")).lower().strip()
                    cmd_ts = data.get("command_ts")
                    desired_flag = data.get("desired")
                    nonce = data.get("nonce")
                    _log(f"[Modbus] Incoming command='{cmd}', desired={desired_flag}, ts={cmd_ts}, nonce={nonce}")
                    if not cmd:
                        _log("[Modbus] empty command; ignoring")
                        return
                    # Build current signature and compare to last seen (include desired)
                    turn_on = (cmd == "on")
                    current_sig = (cmd, turn_on, nonce, cmd_ts)
                    if last_seen_sig[0] == current_sig:
                        _log("[Modbus] duplicate command signature; ignoring")
                        return
                    last_seen_sig[0] = current_sig

                    if cmd not in ("on", "off"):
                        _log(f"[Modbus] unknown command '{cmd}'; skipping")
                        return
                    addr_cmd = doc_to_modbus(COIL_DOC_2070)
                    ok, msg = modbus_write_coil(addr_cmd, turn_on)
                    status_payload = {
                        "command": cmd,
                        "desired": turn_on,
                        "command_ts": cmd_ts,
                        "nonce": nonce,
                        "write_ok": ok,
                        "write_msg": msg,
                        "timestamp": firestore.SERVER_TIMESTAMP,
                        "bridge_version": "unified_v6",
                    }
                    if not ok:
                        _log(f"[Modbus] write failed: {msg}")
                        try:
                            status_ref.set(status_payload, merge=True)
                        except Exception:
                            pass
                        return
                    _log(f"[Modbus] write OK; confirming coil {COIL_DOC_1298}")
                    addr_confirm = doc_to_modbus(COIL_DOC_1298)
                    confirmed = False
                    attempts = 0
                    for i in range(10):
                        attempts = i + 1
                        time.sleep(0.5)
                        state, err = modbus_read_coil(addr_confirm)
                        if err:
                            _log(f"[Modbus] confirm attempt {attempts}: error {err}")
                            continue
                        _log(f"[Modbus] confirm attempt {attempts}: state={state}")
                        if state is not None and bool(state) == bool(turn_on):
                            confirmed = True
                            break
                    status_payload.update({
                        "confirmed": confirmed,
                        "confirm_attempts": attempts,
                    })
                    try:
                        status_ref.set(status_payload, merge=True)
                    except Exception:
                        pass
                    _log(f"[Modbus] result: {'CONFIRMED' if confirmed else 'NOT CONFIRMED'} after {attempts} attempts")
                cmd_ref.on_snapshot(on_snapshot)
                _log("[Modbus] Listener attached to Flights_commands/current_command")
            except Exception as e:
                try:
                    log_print(f"[Modbus] failed to attach listener: {e}")
                except Exception:
                    print(f"[Modbus] failed to attach listener: {e}")

        # Start threads if not already started by existing main
        try:
            _BRIDGE_LISTENER_STARTED
        except NameError:
            try:
                threading.Thread(target=start_modbus_bridge_listener, args=(db, PLC_HOST, PLC_PORT, UNIT_ID), daemon=True, name="ModbusBridgeListener").start()
                _BRIDGE_LISTENER_STARTED = True
                try:
                    log_print("✓ Modbus bridge listener thread started")
                except Exception:
                    print("✓ Modbus bridge listener thread started")
            except Exception as e:
                try:
                    log_print(f"✗ Could not start Modbus bridge listener: {e}")
                except Exception:
                    print(f"✗ Could not start Modbus bridge listener: {e}")

        while True:
            time.sleep(1)
    except Exception as e:
        log_print(f"CRITICAL STARTUP ERROR: {e}")
        input("Press Enter to close...")
