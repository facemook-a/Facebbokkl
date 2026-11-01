# Studio 1.3 security additions

Owner biometric/device authentication unlocks only an existing authorized Firebase owner session. First login and all server/rule administrator claims remain enforced; no public signup or self-granted owner role was added.

Remote code tools run in a non-exported WebView Activity with CSP, network/file/content/storage access disabled and no native/credential bridge. Code/JSON packages cannot load dex/so/APK modules or declare VPN readiness. Remote native actions call existing permission-checked app flows.

The APK updater checks HTTPS, size, digest, package, exact higher version and the current signing certificate, then opens Android's user-confirmed installer through a narrowly scoped private-cache FileProvider. It does not silently install or promise forced relaunch. Public update manifests can be read by signed installations before license activation; clients cannot publish them.

Real-device biometric, code-tool and APK-installation acceptance remains unperformed. Live Firebase deployment, a supported partner SDK and server-enforced VPN entitlement are still external prerequisites. See STUDIO.md and current VERIFICATION.md; prior sections below are historical context.

# Provider release security additions

Version 1.2.0-provider keeps the one-installation Spark schema and default-deny rules. Provider grants are inaccessible to Android clients, including Android admins. Public partner configuration contains no master secrets; Android administrators can toggle only trusted integration-supplied USA location IDs. Trusted Admin SDK integration owns catalog/readiness publication.

The optional managed Functions bridge checks current paid expiry and device binding at grant issue and consumption. Random grants are single-use, stored only as hashes and expire within 60 seconds or the paid expiry. App Check is enforced on issuance; the callback requires a Secret Manager-backed Bearer secret and never logs tokens. No authorized provider secret was supplied or deployed here.

The optional SDK adapter is not verified against a supplied SDK binary. No live service can be enabled until a supported authorized partner SDK/account and provider-enforced expiry/revocation have passed acceptance. The basic OAuth response metadata does not prove active-session termination. Client listeners/polling are additional protection, not authority after process death. See PROVIDER-SETUP.md for required partner contract behavior and unresolved integration work.

The following older gateway/engine notes describe retained **legacy** source, not the default provider Client. No old APK result certifies this version.

# Security boundaries — Spark release

- Administrator rights require Firebase Auth admin:true and are enforced by Firestore rules and the trusted gateway. No public role-grant/self-registration feature exists. The app itself is not the authority.
- The free default uses direct Firestore, not Functions. Rules allow admin-only generation/management, targeted known-code reads without enumeration, atomic one-UID activation, request.time timestamps and server-calculated expiry. Forty random code bits and SHA-256 lookup IDs do not make guessed passwords unlimited-strength; monitor quotas/abuse and configure App Check appropriately. This path has no custom IP rate-limit service.
- One UID means one installation, not tamper-proof physical identity. Rooted/cloned installations or stolen authentication material require threat-model review. Reinstallation requires reset; repeated presence/model fields are not proof of two phones. Legitimate conflict records cannot automatically ban another user's license.
- Spark's free usage quotas are finite. Firestore permission/rate/quota failures are surfaced; no billing is silently enabled. Admin audit events are authenticated but remain controlled by a project administrator, not an independent tamper-proof audit system.
- Service-account JSON, online CA/server keys and management secrets stay root-only on the owned VPS. Firebase Admin SDK bypasses rules and needs careful IAM/rotation. Android sends authenticated ID tokens to the administrator-selected HTTPS gateway; register only hosts you control. Ordinary user settings cannot change this trust boundary.
- Private keys/auth secrets/scripts/include directives are rejected by the public-profile importer. Metadata imports do not prove endpoint control. Safe profile documents are admin-only; published server metadata excludes private notes/secrets. No legacy shared VPN private key is bundled.
- Temporary client profiles/limited metadata use Android Keystore AES-GCM, with backup/transfer disabled and fail-closed encryption. Client process/OS integrity remains required. Public Firebase Android API keys are identifiers, not administrator secrets; preserve valid keys and configure their package/API restrictions.
- Authorization is verified on handshake and periodically during sessions. Session tokens are random256-bit values, only hashes stored, max1h bounded by license. Gateway auth cache≤45s,15s sweep plus network/load delay; expiry is always checked. Per-session certificate revocation and systemd fail-closed behavior need real load/staging acceptance.
- App Check SDK providers are installed but valid real-device registration/service enforcement is an owner deployment step. Already enabled enforcement must remain satisfied. No attestation success or independent security audit is claimed.

## Embedded implementation

Vendored OpenVPN for Android **v0.7.69**, commit `f669267e2547fddca04d0bc699739137085eb633`, with its pinned native dependencies. The native OpenVPN ref is `c29ee42e48df0de5993f7b256829196e67e76006`; OpenSSL is `b280e0c0c81ce6f8d5291c1f8efb6712c357c28a`. Both ABIs are compiled from source, not downloaded unknown AARs.

Integration changes: Android library Gradle module, explicit OpenVPN2-only CMake switch, pinned version metadata for archive builds, Android libgen/dirname configuration, Kotlin Base64 opt-in, Keystore encryption/fail-closed profile persistence, application/manifest/package integration, and removal of unneeded storage/package-visibility permissions. Upstream licenses and full relevant corresponding source are retained. Distribution of a derivative client must comply with upstream GPLv2 terms and its OpenSSL/Apache exceptions; see `OpenVPN-Core/upstream-doc/LICENSE.txt`. Shared/Admin/backend code supplied here is GPL-2.0-or-later with the same applicable linking exceptions; preserve third-party notices.

## Dependency audit limitation

The legacy Functions audit report is stored alongside validation results. The unused legacy Node Firebase Admin/Functions dependencies were updated to current registry versions tested here. npm still reports **six moderate transitive findings** through Firestore's pinned internal google-gax/rimraf/glob/minimatch dependency chain. `npm audit fix` did not resolve them within supported ranges; no unsupported major override or suppression was forced. There were no high or critical npm findings in that run. These findings must be tracked/reassessed against upstream releases before production; a zero-vulnerability or independent security-audit claim is not made.

A software build and isolated tests are not a penetration test, device attestation validation, deployed Firestore acceptance test or VPN leak test. Required staging acceptance is described in FIREBASE.md and GATEWAY.md.

## Published source fixture cleanup

GitHub push protection flagged the upstream OpenSSL apps/rsa8192.pem public test fixture as a private key. The distribution now omits 132 unused upstream sample/test files containing private-key blocks; none are part of the Android compilation targets or operational credentials. See OpenVPN-Core/OMITTED-TEST-KEY-FIXTURES.txt for paths and pinned upstream provenance. Compiled implementation source, public certificate fixtures and license notices remain. Push protection was not bypassed.
