# Verified delivery — 2026-11-01

| Check | Actual result |
|---|---|
| Admin package | com.b5studios.usafavor |
| Client package | com.usafavor.client |
| Firebase selection | usa-favor; exact matching current registration in each app; generated app ID/API-key comparison passed |
| Android build | Both Debug APKs compiled with Kotlin/Compose and embedded native OpenVPN |
| Native ABIs | arm64-v8a and x86_64; engine executables and shared libraries present |
| Android Lint | Both applications: no issues found |
| APK signatures | Verified Android debug signatures; no production signing key supplied |
| Backend tests | 14 passed, 0 failed, 0 skipped on Node 22; domain and isolated handler tests |
| Gateway tests | 3 passed, 0 failed; isolated unit tests |
| Scripts | Node syntax, Python compilation and Bash syntax passed |
| Dependency audit | 6 moderate transitive findings; 0 high, 0 critical; see npm-audit.json and SECURITY.md |
| Production signing guard | Expected rejection of missing B5_KEYSTORE_FILE; no production release claimed |
| Firebase administrative access | CLI projects:list failed authentication; no resources deployed |
| Firestore Emulator integration | Not run; official download blocked by network policy (storage.googleapis.com CONNECT 403) |
| Android device/emulator runtime | Not run; adb listed no devices; /dev/kvm unavailable |
| US VPS / live VPN / DNS/IPv6/revocation acceptance | Not run; no operational VPS or deployment credentials provided |

Logs, package/signature reports and SHA-256 digests are in [Validation](Validation/). These are test APKs, not a claim of a verified production service. No application data was seeded to fabricate statistics, licenses or servers. The old VPN profile and private key were not shipped or connected to.

The source archive includes matching Firebase Android configuration (public project/app identifiers), but no VPS, issuer, backend service-account or production-signing secrets. Keep the Firebase owner/IAM/App Check and gateway prerequisites in FIREBASE.md and GATEWAY.md separate from the successful compiler/unit checks.

Reusable cloud-environment `install_script` and `start_skill` were saved to the draft, replacing obsolete static-site startup guidance. Saving does not publish the snapshot; review/save the environment settings and publish when you want future tasks to use this prepared toolchain.
