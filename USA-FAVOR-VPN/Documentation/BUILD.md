# Build 1.3.0-studio

Use JDK 21 (a full JDK including javac), Android SDK Platform 35 and Build Tools 35.0.0, and the checksum-pinned Gradle 8.13 wrapper. Default builds no longer compile OpenVPN or require NDK/CMake. Keep SDK license, TLS and checksum verification enabled. Set JAVA_HOME and ANDROID_HOME; GRADLE_USER_HOME is optional.

Both package IDs remain unchanged. Place the original matching public Android registrations in `Admin-App/app/google-services.json` and `Client-App/app/google-services.json`. They can also be restored from the provided old source ZIP. No service-account or provider master credentials belong in these files.

```sh
python3 tools/verify-firebase.py
./tools/build.sh
./gradlew -PvisualReview=true :Admin-App:recordPaparazziDebug :Client-App:recordPaparazziDebug
```

`tools/build.sh` builds both Debug apps, runs both lint checks and copies test APKs/checksums to artifacts/. `tools/cloud-build.sh` supports this workspace's toolchains, session proxy and configured system CA trust. Each task is already isolated; use the existing checkout, not a new Git worktree unless explicitly requested.

Default APKs intentionally have no live VPN engine without a supported authorized provider SDK. See PROVIDER-SETUP.md to build the optional Aura adapter with the partner-supplied pinned artifact. Compile and validate that adapter against the exact supported SDK before publishing live service. A successful default APK build does not validate the optional SDK, Firebase deployment or real tunnel.

For production use `tools/build-release.sh` with B5_KEYSTORE_FILE, B5_KEYSTORE_PASSWORD, B5_KEY_ALIAS and B5_KEY_PASSWORD supplied securely. Retain the existing production key for updates. New local Debug keys may differ from the old downloaded APKs; compare fingerprints before attempting an in-place update. A reinstall requires binding reset for test licenses.

Existing source archives and APKs in Downloads predate this update and are retained as historical artifacts. Read the current VERIFICATION.md before claiming a new build or connection passed.
