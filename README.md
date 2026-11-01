# USA FAVOR VPN — B5 STUDIOS

Native Android Admin and Client applications, Firebase backend and deployable OpenVPN gateway.

- [USA-Admin.apk — Debug test build](USA-FAVOR-VPN/Downloads/USA-Admin.apk)
- [USA-Favor.apk — Debug test build](USA-FAVOR-VPN/Downloads/USA-Favor.apk)
- [Source ZIP, including the matching Firebase Android registrations](USA-FAVOR-VPN/Downloads/USA-FAVOR-VPN-source.zip)
- [Source and setup documentation](USA-FAVOR-VPN/README.md)
- [Actual validation results and remaining deployment steps](USA-FAVOR-VPN/Documentation/VERIFICATION.md)
- [SHA-256 checksums](USA-FAVOR-VPN/Downloads/SHA256SUMS.txt)

The APKs are debug-signed test builds. Both compiled and passed Android Lint; 14 backend and 3 gateway unit tests passed. A live VPN connection was not verified. Firebase deployment, App Check, a real US VPS and production signing remain required. The security report documents six moderate transitive npm findings.

Local Firebase Android JSON copies are excluded from Git and included in the source ZIP. No VPS, service-account, issuer or production-signing private keys are supplied. Read the embedded OpenVPN license before distributing derivative binaries.
