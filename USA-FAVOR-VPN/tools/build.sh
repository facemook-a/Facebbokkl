#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
python3 tools/verify-firebase.py
# Recreate APK containers so incremental packaging does not retain obsolete ZIP padding.
python3 - <<'PACK'
from pathlib import Path
import shutil
for module in ['Admin-App','Client-App']:
    root=Path(module)/'app/build'
    for p in (root/'outputs/apk/debug').glob('*.apk'): p.unlink()
    p=root/'intermediates/incremental/packageDebug'
    if p.exists(): shutil.rmtree(p)
PACK
./gradlew :Admin-App:assembleDebug :Client-App:assembleDebug :Admin-App:lintDebug :Client-App:lintDebug "$@"
mkdir -p artifacts
cp Admin-App/app/build/outputs/apk/debug/Admin-App-debug.apk artifacts/USA-Admin.apk
cp Client-App/app/build/outputs/apk/debug/Client-App-arm64-v8a-debug.apk artifacts/USA-Favor.apk
cp Client-App/app/build/outputs/apk/debug/Client-App-x86_64-debug.apk artifacts/USA-Favor-x86_64.apk
python3 tools/verify-firebase.py
sha256sum artifacts/USA-Admin.apk artifacts/USA-Favor.apk artifacts/USA-Favor-x86_64.apk > artifacts/SHA256SUMS
