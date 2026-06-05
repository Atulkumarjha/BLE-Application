#!/usr/bin/env bash
# Install an APK to a connected Android device and capture adb logcat to logs/adb_logcat.log
# Usage: ./tools/install_and_log.sh path/to/app-debug.apk

set -euo pipefail

ROOT_DIR="$(pwd)"
LOGS_DIR="$ROOT_DIR/logs"
mkdir -p "$LOGS_DIR"

APK_PATH="${1:-$ROOT_DIR/build/app/outputs/flutter-apk/app-debug.apk}"

if [ ! -f "$APK_PATH" ]; then
  echo "APK not found at: $APK_PATH" >&2
  echo "Provide path as first argument or build the APK and place it at build/app/outputs/flutter-apk/app-debug.apk" >&2
  exit 2
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found on PATH. Install Android platform-tools." >&2
  exit 2
fi

echo "Installing APK: $APK_PATH"
adb install -r "$APK_PATH"

echo "Installed. Starting adb logcat to $LOGS_DIR/adb_logcat.log (Ctrl+C to stop)"
adb logcat -v time > "$LOGS_DIR/adb_logcat.log"
