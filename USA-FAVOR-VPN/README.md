# USA FAVOR VPN — B5 STUDIOS

Two independent native Kotlin/Compose Android applications, a **Firebase Spark licensing system without Cloud Functions**, and deployable OpenVPN gateway source. Version 1.1.0-spark uses live Firestore rules for license authorization; no billing account is required for the license workflow within Spark quotas. VPS hosting is separate.

| Folder | Purpose |
|---|---|
| Admin-App/app | B5 STUDIOS ADMIN · com.b5studios.usafavor |
| Client-App/app | USA FAVOR VPN · com.usafavor.client · English interface |
| Shared | Native design, cryptographic license generator, Firestore repository and Keystore metadata |
| Firebase-Backend | Spark rules, emulator integration tests; optional legacy Functions source |
| VPN-Gateway | Ubuntu OpenVPN deployment, per-session credentials and trusted license enforcement |
| Assets | Original app icons, design resources, Cairo font and third-party notices |
| Documentation | Free setup, build, security and actual validation results |
| OpenVPN-Core | Vendored OpenVPN for Android v0.7.69 and native implementation sources |

Download the test APKs in [Downloads](Downloads/). Start with [FREE-SETUP.md](Documentation/FREE-SETUP.md) to create the free database, publish the supplied rules and enable anonymous Client authentication. Your existing administrator claim remains necessary. A previous Functions-backed release is not compatible with this new schema without migration; do not deploy the legacy Functions configuration against the Spark schema.

The Admin generates random USA-XXXX-XXXX codes, sets generation/first-activation durations, manages licenses and bindings, shows rejected second-installation attempts, and controls remote content, recognizable social links, dashboard sections and verified gateway inventory. The Client has an English interface, original shield art, animated native controls and Keystore-protected tunnel profiles. Every authorization still depends on Firestore rules and, for VPN use, the trusted gateway.

Ordinary compatible server and content changes arrive remotely. Adding a record or importing a profile does not provision or take control of a server. The uploaded legacy OpenVPN profile contains a shared private key and obsolete encryption; it is not bundled or treated as proof of ownership. A compatible gateway must be deployed or adapted by its authorized operator. See [GATEWAY.md](Documentation/GATEWAY.md).

[BUILD.md](Documentation/BUILD.md) describes reproducible Android builds; [VERIFICATION.md](Documentation/VERIFICATION.md) records actual results. The delivered APKs are debug-signed test builds. No live VPS tunnel, deployed project rules, or production signing credentials were available for verification in this environment. See [SECURITY.md](Documentation/SECURITY.md) for quotas, binding limitations and operational boundaries. Preserve the corresponding OpenVPN source and license notices when distributing the client.
