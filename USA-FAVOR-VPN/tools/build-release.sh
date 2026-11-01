#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
python3 tools/verify-firebase.py
./gradlew :Admin-App:assembleRelease :Client-App:assembleRelease :Admin-App:lintRelease :Client-App:lintRelease "$@"
mkdir -p artifacts/release
cp Admin-App/app/build/outputs/apk/release/Admin-App-release.apk artifacts/release/USA-Admin.apk
cp Client-App/app/build/outputs/apk/release/Client-App-arm64-v8a-release.apk artifacts/release/USA-Favor.apk
cp Client-App/app/build/outputs/apk/release/Client-App-x86_64-release.apk artifacts/release/USA-Favor-x86_64.apk
sha256sum artifacts/release/*.apk > artifacts/release/SHA256SUMS
