# WAVE ECU RACING — Android source

## Included
- Kotlin + Jetpack Compose Android application
- Dark/green Racing dashboard for Honda Wave 110i
- CSV import for telemetry fields matching the supplied log format
- RPM / AFR / TPS graph from imported log points
- In-session Data Log
- Read-only MAP profile list / draft creation

## Safety and hardware status
This build is intentionally READ ONLY. It does not connect to an ECU, send tuning commands, write fuel/ignition tables, change rev limits, or provide performance shift prompts. No physical adapter was specified, and Honda Wave 110i ECU connectors/protocols can vary by model year and ECU variant. Do not buy an adapter until the exact ECU part number, model year, connector and supported protocol are confirmed.

The LIVE label is reserved but no real-time hardware adapter is implemented. Imported CSV is historical data, not live telemetry. The dashboard starts in DEMO DATA mode.

## Build APK
1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this folder (`WaveEcuRacing`) in Android Studio.
3. Let Gradle sync.
4. Use Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Command line (with Android SDK + Gradle installed):
`gradle assembleDebug`

## CSV format
The importer expects the supplied telemetry headers, including:
`RPM, TPS_Percent, AFR_Wideband, Speed_kmh, ECT_Coolant_degC, IAT_Intake_degC, MAP_Pressure_kPa, Battery_Voltage_V, Injector_Pulse_ms, Ignition_BTDC_deg, Time_ISO`.

## Known first-version limits
- Logs stay in app memory for the current session; persistent database/export is not yet added.
- MAP profiles are local draft labels, not ECU binary map files.
- Live Bluetooth/USB/OBD protocol adapter is not implemented.
- CSV parser supports quoted fields and comma separators; it expects a header row.
