# Validation and limits

## Executed checks

The exact final results/logs are in `Documentation/Validation/` and `VERIFICATION.md`.

- Both native Android Debug builds, with package-matched Firebase registrations and native OpenVPN libraries for arm64-v8a/x86_64.
- Android Lint for both applications.
- Node 22 backend domain and isolated callable-handler tests (14). The handler suite uses an explicit in-memory transaction test double; it does not prove deployed Firestore concurrency or HTTP App Check behavior.
- Python gateway unit tests (3), Python compilation and Bash installation-script syntax.
- APK package identity, launcher/service manifest, signatures and native-library presence; generated Firebase app ID/key selection checks without printing keys.
- npm audit, including the documented unresolved transitive moderate findings. No security findings were suppressed.

## Run locally

```sh
cd Firebase-Backend/functions
npm ci --ignore-scripts
npm test
npm run check
cd ../../VPN-Gateway
python3 -m unittest -v
python3 -m py_compile agent.py auth-hook.py
bash -n install.sh
```

For real local Firestore transactions/rules, start the Firestore Emulator from Firebase-Backend with project `demo-usa-favor` and the supplied firebase.json/rules. Then:

```sh
GCLOUD_PROJECT=demo-usa-favor FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node --test functions/test/emulator.integration.js
```

The integration script refuses production projects and remote emulator hosts. It tests actual transaction exclusivity and denies unauthenticated REST database reads. It was **not run here**: the environment denied the official Firestore Emulator download from `storage.googleapis.com` with HTTP CONNECT 403. Enable that domain in environment networking if you want to run it in this cloud workspace. Do not bypass the proxy or disable verification.

No Android emulator/device was attached; `/dev/kvm` was unavailable. Android application launch/interaction, Play Integrity/debug-token registration, Firebase deployment/IAM/Auth provider readiness, Ubuntu installation/reboot/firewall behavior, full VPN routing, egress/DNS/IPv6 checks, network-change handling, expiry/revocation timing and a real US tunnel remain staging acceptance work. There is no claim of verified Connected state, operational VPS or pixel-perfect/device-rendered design comparison.
