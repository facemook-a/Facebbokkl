# Build native Android applications

Both applications are independent APKs in one reproducible Gradle workspace. The requested `app/` layout is used for each:

```
Admin-App/app/      com.b5studios.usafavor
Client-App/app/     com.usafavor.client
Shared/            Compose design, backend repository, secure metadata
OpenVPN-Core/      vendored maintained engine and native sources
```

Use JDK 17+ (JDK 21 tested), Android SDK Platform 35, Build Tools 35.0.0, NDK 28.0.13004108, CMake 3.22.1 and the checked-in Gradle 8.13 wrapper. The official Gradle distribution SHA-256 is pinned. SDK licenses must be accepted with Android's sdkmanager. Keep TLS/checksum verification enabled. Set `ANDROID_HOME`, `JAVA_HOME` and optionally `GRADLE_USER_HOME` for your workstation. No external OpenVPN companion app or prebuilt opaque VPN AAR is required. The core is compiled into separate arm64-v8a and x86_64 Client APKs to reduce download size. Minimum Android version is 8.0 / API 26; 32-bit-only devices are not included in these APKs.

```sh
python3 tools/verify-firebase.py
./tools/build.sh
```

The two Firebase JSON files must match their exact package/project. They are excluded from Git as local configuration but included in the supplied source archive. This avoids accidentally committing user environment configuration. Debug builds select Firebase Debug App Check; register test installation tokens before live backend use.

`tools/build.sh` builds both Debug APKs, runs both Android Lint checks, verifies selected Firebase resources, and writes:

- artifacts/USA-Admin.apk
- artifacts/USA-Favor.apk (arm64-v8a; normal modern Android phones)
- artifacts/USA-Favor-x86_64.apk (optional emulator/x86 target)
- artifacts/SHA256SUMS

In this prepared cloud workspace, `tools/cloud-build.sh` uses the installed JDK/SDK/cache and derives Java proxy host/port from the inherited HTTPS proxy without printing credentials. Do not disable proxy CA trust or TLS verification. Future isolated tasks should use the existing checkout, not create Git worktrees unless explicitly requested.

## Production signing

The delivered APKs are **debug-signed test builds**, not a privately production-signed release. You must retain your own production signing keystore securely and register its fingerprints in Firebase/App Check. Set these environment variables securely on your build machine:

- B5_KEYSTORE_FILE
- B5_KEYSTORE_PASSWORD
- B5_KEY_ALIAS
- B5_KEY_PASSWORD

Do not save password values in source, Gradle properties, command history or chat. Then run `./tools/build-release.sh`; outputs go to `artifacts/release/`. Release tasks reject missing Firebase registration or signing requirements. Keep the signing key stable for updates. A debug-signed installation cannot be upgraded with a different production key; use a fresh installation and reset a test license binding if it was activated during debugging. The source and build configuration are provided; no production signing credentials were supplied or invented.

Before distribution, perform the physical-device/Firebase/gateway acceptance in FIREBASE.md and GATEWAY.md. Check Android's VPN/foreground-service/store disclosures and the privacy information against the service actually deployed. The source archive includes upstream license notices and must remain available as required by the embedded engine's license.

Version 1.1.0-spark uses free direct Firestore licensing. Follow FREE-SETUP.md rather than deploying legacy Functions. UI resources use bundled Cairo typography, original adaptive icons and recognizable Material/social glyphs. Client resources and layout are English/LTR. Optional rendering checks: run `./gradlew -PvisualReview=true :Shared:recordPaparazziDebug` with the same JDK/SDK/proxy configuration. Rendering fixtures do not test live authentication or a tunnel.
