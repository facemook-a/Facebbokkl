# USA FAVOR VPN — B5 STUDIOS

Version **1.1.0-spark**: two native Android apps with free Firestore licensing, an English Client interface, original adaptive icons and animated Compose design.

- [USA-Admin.apk — Debug test build](USA-FAVOR-VPN/Downloads/USA-Admin.apk)
- [USA-Favor.apk — ARM64 Debug test build](USA-FAVOR-VPN/Downloads/USA-Favor.apk)
- [Optional x86_64 Client APK](USA-FAVOR-VPN/Downloads/USA-Favor-x86_64.apk)
- [Complete source ZIP with matching Firebase Android registrations](USA-FAVOR-VPN/Downloads/USA-FAVOR-VPN-source.zip)
- [Free setup — شرح بالدارجة خطوة بخطوة](USA-FAVOR-VPN/Documentation/FREE-SETUP.md)
- [Source and deployment documentation](USA-FAVOR-VPN/README.md)
- [Actual verification results](USA-FAVOR-VPN/Documentation/VERIFICATION.md)
- [SHA-256 checksums](USA-FAVOR-VPN/Downloads/SHA256SUMS.txt)

The default license workflow needs Firebase Spark, a Standard Native(default) Firestore database with the supplied rules, admin:true authorization and Anonymous Auth for Client. It does not require Cloud Functions or billing, within free quotas. One code binds to one installation; another installation is refused and can be reviewed/banned by Admin.

Both APKs compile and pass lint. Sixteen real Firestore-emulator security cases, nine isolated gateway tests and two native component-rendering tests passed. These are debug-signed test APKs; no real VPS tunnel, deployed Firebase setup or production signing was verified here. The legacy uploaded third-party OVPN profile does not prove gateway control and is not bundled. See the verification report for exact scope and remaining setup.

No VPS, service-account, issuer or production-signing private keys are supplied. Local Firebase Android registrations are excluded from Git and included in the source archive. Preserve OpenVPN's corresponding source and license notices on distribution.
