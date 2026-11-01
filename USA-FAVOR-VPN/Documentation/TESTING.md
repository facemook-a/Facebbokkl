# Validation

The active path is Spark rules plus a trusted owned gateway. See VERIFICATION.md and Validation/ for retained evidence.

- Android: `./tools/build.sh` (or cloud-build.sh in the prepared workspace) compiles both APKs, runs Android Lint and verifies matching Firebase registration selection.
- Firestore: with Node24.12+ and Java21+, run `npm ci --ignore-scripts --prefix Firebase-Backend/rules-tests`; from Firebase-Backend run `./rules-tests/node_modules/.bin/firebase emulators:exec --project demo-usa-favor --only firestore "npm test --prefix rules-tests"`. This launches the official emulator and runs16 integration cases: roles, no inventory enumeration, atomic two-device race, binding/expiry/timestamp forgery denial, reset anchor, conflicts and private content/profile restrictions. GitHub Actions runs this read-only workflow and uploads logs as artifacts. Local emulator download is blocked by this environment's storage.googleapis.com policy; the recorded passing run was executed on GitHub's runner.
- Gateway: from VPN-Gateway, `python3 -m unittest -v`, `python3 -m py_compile agent.py firebase_store.py auth-hook.py`, `bash -n install.sh`. Nine tests exercise expiry/token/policy authorization and gateway helpers, with isolated data doubles rather than a deployed Firestore/VPS.
- Design: optional `./gradlew -PvisualReview=true :Shared:recordPaparazziDebug`, same Android environment/proxy. Two native Compose rendering fixtures produce actual component images; they do not simulate a VPN or prove full app navigation.
- Package: inspect version/package/native ABI, verify APK signatures, compare old/new debug signing certificate and scan APK entries for unbundled profile/private-key resources.

Legacy functions/ tests remain for reference but are not the active Spark runtime. Their old npm audit belongs to that unused source tree. Real project Auth/rules deployment, App Check attestation, physical-device behaviors, production signing and live tunnel/leak/revocation acceptance remain external checks in FIREBASE.md and GATEWAY.md.
