# Current validation — 1.3.0-studio

| Check | Result |
|---|---|
| App identity | Client launcher label USA VPN; package com.usafavor.client unchanged. Admin com.b5studios.usafavor. Both versionCode 5 / 1.3.0-studio |
| Android tests | 31 passed, zero failures/errors/skips: 15 native Compose renders and 16 configuration/content/package-policy JVM cases |
| Provider bridge | 10 passed, zero failures/skips: license/expiry/binding/readiness/maintenance policy and actual local callable/HTTP failure paths |
| Debug APK build | Both Kotlin/Compose applications built with the checksum-pinned Gradle wrapper; current build log in Validation/studio-1.3 |
| Lint | Both apps: zero errors and zero warnings in final Android Lint reports |
| Activation | First valid unused code binds atomically to one anonymous installation; subsequent restoration preserves original expiry. Current live Firebase activation not verified |
| Firestore tests | **26 passed, 0 failed** on the official emulator in [GitHub Actions run 41332145781](https://github.com/facemook-a/Facebbokkl/actions/runs/41332145781), tested commit c11031d. Local emulator download remains network-blocked; exact tested rules SHA-256 is recorded in Validation/studio-1.3/firestore-ci.json |
| Arabic and owner unlock | Arabic/English preference, RTL and biometric/device-credential unlock of an already authorized owner session implemented; sensor/system-auth acceptance on a physical phone remains untested |
| Sections and packages | Native actions, isolated HTML/CSS/JS tools and validated JSON import implemented; real-device WebView execution not exercised by Compose snapshots |
| Updates | In-app HTTPS download, bounded redirects/size, SHA-256, package/version/current signing-key checks and user-confirmed Android installer implemented; physical installation/relaunch not tested |
| Package/signature/integrity | Verify current APK metadata, signatures, ZIP checksums and release SHA256SUMS before distributing; Debug keys differ from the old preexisting Downloads |
| Live service | No authenticated Firebase deployment, supported Aura artifact/account or live VPN/US egress supplied or validated. The default Client has no provider engine and stays disconnected |

Current verification is local build/policy/render evidence. It is not deployed licensing acceptance, a physical-device installation/biometric test or provider entitlement/VPN leak testing. See STUDIO.md for exact capabilities and boundaries. The optional Aura SDK source requires a supported partner artifact and version-specific validation; no APK/download/config package can add an absent native SDK automatically.

---

## Historical 1.2.0 and 1.1.0 evidence

# Current validation — 1.2.0-provider — 2026-11-01

| Check | Current result |
|---|---|
| Android Debug APKs | Both compiled; unchanged IDs com.b5studios.usafavor and com.usafavor.client, versionCode 3 / 1.2.0-provider |
| Android lint | Both apps: zero issues |
| Native UI rendering | 9 passed: 4 Admin, 3 Client, 2 Shared; actual Compose screenshots, not physical-device acceptance |
| Provider settings JVM policy | 4 passed; missing/unverified provider fails closed, only enabled USA catalog locations offered |
| Optional provider Functions | 9 passed: authorization/expiry/device/disabled-location policy and real local callable/HTTP rejection paths; no live Firestore/provider access claimed |
| Retained gateway helper tests | 9 passed; legacy Python helpers only, not a live gateway |
| Firebase registrations | Original public Android registrations restored locally from supplied source archive; exact project/package/generated resources verified without printing key values |
| Firestore rules tests | 22 defined; current run **blocked before execution** by proxy denial downloading the official emulator from storage.googleapis.com; previous 16-pass CI evidence below is historical |
| Rules parity | Canonical Firestore rules match app-packaged copy byte for byte |
| APK integrity | ZIP integrity, checksums and APK v2 signatures verified; no private .ovpn/.key/.pem/.p12/.pfx entries |
| Signing continuity | New local Debug certificate differs from old Downloads APK; in-place update requires original signing key. Do not uninstall a licensed production installation merely to test these APKs |
| Optional Aura SDK adapter | Source provided; not compiled or verified because no current authorized SDK artifact/account was supplied; old linked public SDK repos return not-found |
| Live Firebase deployment/activation | Not performed; no authenticated project administrator runtime supplied |
| Real VPN / USA egress / per-app leaks / provider expiry enforcement | Not verified; no authorized provider connection. UI deliberately shows Provider not connected |
| Production signing | Not supplied; artifacts are Debug test APKs |

Fresh build outputs are in artifacts/. Four review images are in artifacts/previews/. Current logs and test counts are in Validation/provider-1.2/. Existing Downloads remain historical and were not overwritten.

The implemented Firebase fixes add licensed reads of the new provider settings document, restrict Android location writes to the trusted USA catalog, keep grants server-only, and stop swallowing genuine licenseInfo permission errors as “not activated.” These changes still need rule publication, correct admin claims and App Check registration on the live project. A saved network-domain draft does not change runtime access; apply the saved storage.googleapis.com allowance in environment settings to run the real Firestore suite, then rerun its documented command.

See PROVIDER-SETUP.md for documented partner authorization, SDK and provider-side entitlement enforcement prerequisites. No active-session expiry/revocation guarantee is inferred from the basic OAuth callback. No master provider credential is shipped in either app.

---

## Historical 1.1.0 delivery (does not validate the current revision)

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
