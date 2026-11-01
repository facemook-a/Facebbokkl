#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
python3 tools/verify-firebase.py
./gradlew :Admin-App:assembleDebug :Client-App:assembleDebug :Admin-App:lintDebug :Client-App:lintDebug "$@"
mkdir -p artifacts
cp Admin-App/app/build/outputs/apk/debug/Admin-App-debug.apk artifacts/USA-Admin.apk
cp Client-App/app/build/outputs/apk/debug/Client-App-debug.apk artifacts/USA-Favor.apk
python3 tools/verify-firebase.py
sha256sum artifacts/USA-Admin.apk artifacts/USA-Favor.apk > artifacts/SHA256SUMS
