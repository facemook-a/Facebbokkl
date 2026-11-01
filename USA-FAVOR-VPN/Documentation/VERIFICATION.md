# Verified delivery — 1.1.0-spark — 2026-11-01

| Check | Actual result |
|---|---|
| Admin | com.b5studios.usafavor, versionCode2, debug-signed; 7,244,214 bytes (6.91MiB) |
| Client | com.usafavor.client, versionCode2, English/LTR; ARM64 10,697,732 bytes (10.20MiB) |
| Optional Client | Separate x86_64 APK 10,980,509 bytes (10.47MiB) |
| Final Android build | Both Kotlin/Compose APKs with embedded Client OpenVPN compiled successfully |
| Android Lint | Both applications: zero errors and zero warnings |
| Firebase configuration | usa-favor; exact matching current app registrations and generated app-ID/API-key comparison passed, without printing key values |
| APK package/signature | Version/package, ZIP integrity and native ABI checked; signatures verified; same debug certificate as previous delivery supports in-place updates |
| Private-profile packaging | No .ovpn/.key/.pem/.p12/.pfx entries in delivered APKs; uploaded shared-key legacy profile excluded |
| Firestore integration | **16 passed, 0 failed** on the official emulator in GitHub Actions [run41288670814](https://github.com/facemook-a/Facebbokkl/actions/runs/41288670814), commit0d1101e; final canonical and app-packaged rules match that tested rules hash |
| Local Firestore emulator | Download blocked by storage.googleapis.com network policy; no local integration-run claim |
| Gateway | **9 passed, 0 failed**, isolated Python authorization/helper tests; Python compilation and installer Bash syntax passed |
| Native Compose rendering | **2 passed, 0 failed, 0 skipped** via Paparazzi; component images in Shared/src/test/snapshots/images, not a full-device acceptance test |
| Active license backend | Direct Firestore Spark rules/transactions; no Cloud Functions/billing/TTL deployment required within free quotas |
| Firebase deployment | Not performed in this workspace; owner must create(default), publish rules and enable Auth providers |
| Live VPN / DNS / IPv6 / revocation | Not tested against a real owned gateway or physical Android device; third-party OVPN metadata is not proof of management/location |
| Production signing | Not supplied; these are Debug test builds, not production-signed releases |
| Legacy Functions audit | Retained unused source reports six moderate transitive npm findings; not the active Spark runtime |

Evidence is in [Validation/](Validation/): final build/lint, package/manifests/signatures/checksums, gateway logs, native rendering results and the observed public CI result with the SHA-256 of the tested rules. Full CI logs are uploaded by the read-only workflow as a GitHub artifact (seven-day retention). No live backend data was seeded to fabricate customers, statistics, IPs or servers.

The free license workflow still requires owner setup in [FREE-SETUP.md](FREE-SETUP.md). Spark quotas are finite. One-code binding means one installation UID; reinstalling needs an admin reset. The admin reviews rejected second-installation attempts and chooses whether to ban, preventing an attacker from automatically disabling someone else's code by attempting activation.

Real VPN service requires authorized gateway management and acceptance in [GATEWAY.md](GATEWAY.md). No claim of free unlimited hosting, guaranteed US platform content, tested Play Integrity or verified tunnel is made. Source archives contain matching Firebase Android public project/app registrations but no VPS, issuing CA, backend service-account or production signing keys.

The reusable environment install_script and start_skill were saved for the Android/Spark workflow; saving a draft does not publish the environment snapshot or deploy Firebase. Automatic approval review rejected a proposed CI repository-write logging method. It was replaced with read-only CI annotations/artifacts; no persistent CI repository-write permission was granted.
