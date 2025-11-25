Modbus / Firestore / Drive Bridge

Quick start

1) Install Python dependencies (prefer a virtualenv):

```bash
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

2) Prepare a Firebase service account JSON and put it on the machine. Example path: C:\secrets\firebase-sa.json

3) Run the bridge (example):

```bash
python tools\modbus_firestore_drive_bridge.py \
  --service-account C:\secrets\firebase-sa.json \
  --plc-host 192.168.1.50 --plc-port 502 \
  --drive-file-id 1eEX... \
  --poll-interval 1 \
  --fire-doc scada_controls/lights \
  --status-doc scada_controls/lights_status \
  --coil-write 2070 --coil-confirm 1298
```

Notes
- The script will poll Firestore for a `command` (string) or `desired` (boolean) in the `scada_controls/lights` document. It also optionally polls a Drive text file if you pass `--drive-file-id`.
- When a command is observed, the script writes the target Modbus coil (M21 address 2070) and then reads the confirmation coil (address 1298) and writes status back to `scada_controls/lights_status`.
- If your PLC uses 1-based Modbus addressing, pass `--modbus-base 1`.
- Keep the service account JSON safe; it grants access to your Firestore and Drive data.

Troubleshooting
- If you see Modbus connection warnings, verify PLC IP/port and firewall.
- If Firestore calls fail, verify the service account has the proper Firestore permissions.
- To test without a PLC, run the script without `--plc-host`; it will log and retry but you can watch Firestore updates.

