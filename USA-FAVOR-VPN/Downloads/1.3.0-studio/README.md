# 1.3.0-studio — Debug test downloads

- [USA VPN client, ARM64](USA-VPN.apk)
- [B5 STUDIOS Admin](USA-Admin.apk)
- [Client x86_64, optional emulator](USA-VPN-x86_64.apk)
- [Complete project/APK ZIP](USA-FAVOR-Complete.zip)
- [SHA-256 checksums](SHA256SUMS.txt)
- [Native Compose review images](Previews/)

Client launcher label is USA VPN. Packages remain com.usafavor.client and com.b5studios.usafavor; versionCode 5. Initial owner authentication remains required; later unlock can use Android biometrics/device credential. Clients activate a code once on one installation until expiry.

These APKs build successfully and have local policy/render verification. Firebase deployment, real-device biometric/installer/WebView acceptance and authorized provider VPN connectivity are not verified. Default client has no configured VPN provider SDK and shows service unavailable. Debug signing differs from older historical Downloads APKs; in-place updates need the original signing key.

Read [Studio features](../../Documentation/STUDIO.md) and [current verification](../../Documentation/VERIFICATION.md). Code tools/JSON packages change supported content and actions; they cannot add an absent native provider SDK or silently install updates.
