# Security model and release work

- No customer VPN/shared uploaded private keys, VPS credentials or service-account keys are bundled in Android. Firebase Android configuration is a project identifier, protected by backend authorization, strict rules, App Check and key/application restrictions.
- Admin Auth requires an explicit backend custom claim. No client-visible registration or role-granting API exists. Callable APIs require authenticated identities and App Check; UID/IP rate counters use transactions and Firestore TTL. Gateway authorization is separately authenticated using a 256-bit per-gateway key held in Secret Manager/root-only VPS configuration.
- Firestore denies all direct access. Only Functions service accounts access licenses, bindings, inventory, settings, sessions and audits. License lookup identifiers and session token hashes reduce accidental disclosure. The private administrator list intentionally retains raw sale codes for copying/sharing.
- TLS, package signatures, official download checksums and certificate verification remain enabled. Gateway DNS is resolved and checked against public IPv4 only, and the validated address is pinned to that outbound HTTPS request to limit SSRF/rebinding. No arbitrary shell scripts/code can be supplied through remote configuration or OVPN imports.
- Server profiles require modern AEAD settings. Import rejects private keys, credentials, hooks, management/proxy/include directives, unsupported options and oversized files. It returns metadata only and grants no gateway control.
- Client metadata and serialized temporary VPN profiles use Android Keystore AES-GCM. Backup and device transfer are excluded. The upstream skeleton's plaintext profile persistence was replaced, including removal of its encryption-failure plaintext fallback. The client still depends on OS/process integrity; App Check is not a substitute for license enforcement.
- Session credentials expire after at most one hour and are bounded by subscription expiry. Per-session EC client certificates expire in one day and are also gated by the shorter backend token. Client private keys are removed from issuer storage after delivery; certificates/serial database remain for revocation. The online issuing CA and VPN server keys stay root-only on the gateway, not in Firestore. Protect issuer backups and automate expiry/CRL monitoring.
- Revocation has polling latency. The agent revalidates established sessions, terminates unauthorized clients, refreshes CRLs, stops the daemon if its enforcement channel repeatedly fails, and is a required systemd dependency. These mechanisms still require a real staging test and load measurements.

## Embedded implementation

Vendored OpenVPN for Android **v0.7.69**, commit `f669267e2547fddca04d0bc699739137085eb633`, with its pinned native dependencies. The native OpenVPN ref is `c29ee42e48df0de5993f7b256829196e67e76006`; OpenSSL is `b280e0c0c81ce6f8d5291c1f8efb6712c357c28a`. Both ABIs are compiled from source, not downloaded unknown AARs.

Integration changes: Android library Gradle module, explicit OpenVPN2-only CMake switch, pinned version metadata for archive builds, Android libgen/dirname configuration, Kotlin Base64 opt-in, Keystore encryption/fail-closed profile persistence, application/manifest/package integration, and removal of unneeded storage/package-visibility permissions. Upstream licenses and full relevant corresponding source are retained. Distribution of a derivative client must comply with upstream GPLv2 terms and its OpenSSL/Apache exceptions; see `OpenVPN-Core/upstream-doc/LICENSE.txt`. Shared/Admin/backend code supplied here is GPL-2.0-or-later with the same applicable linking exceptions; preserve third-party notices.

## Dependency audit limitation

The final audit report is stored alongside validation results. Firebase Admin/Functions were updated to current registry versions tested here. npm still reports **six moderate transitive findings** through Firestore's pinned internal google-gax/rimraf/glob/minimatch dependency chain. `npm audit fix` did not resolve them within supported ranges; no unsupported major override or suppression was forced. There were no high or critical npm findings in that run. These findings must be tracked/reassessed against upstream releases before production; a zero-vulnerability or independent security-audit claim is not made.

A software build and isolated tests are not a penetration test, device attestation validation, real Firestore transaction integration test or VPN leak test. Required staging acceptance is described in FIREBASE.md and GATEWAY.md.

## Published source fixture cleanup

GitHub push protection flagged the upstream OpenSSL apps/rsa8192.pem public test fixture as a private key. The distribution now omits 132 unused upstream sample/test files containing private-key blocks; none are part of the Android compilation targets or operational credentials. See OpenVPN-Core/OMITTED-TEST-KEY-FIXTURES.txt for paths and pinned upstream provenance. Compiled implementation source, public certificate fixtures and license notices remain. Push protection was not bypassed.
