# Supplied inputs

All ZIP members were inspected. The instructions are reference material; the current request governs the implementation.

| Input | Finding | Action |
|---|---|---|
| Codix-Prompt-Admin.txt | Java/basic `codes` collection instructions | Replaced with Kotlin/Compose and server-managed `licenses` |
| Codix-Prompt-User.txt | Local SharedPreferences expiry, direct Firestore writes, placeholder VPN and wrong package | Rejected; backend/device binding/gateway authorization implemented |
| README.txt | Original project outline | Used as historical context |
| US-NewYork.ovpn | SoftEther/VPN Gate sample, TCP endpoint vpn318641113.opengw.net:1923, AES-128-CBC, SHA1, shared embedded private key and dummy VPNGateClientCert | Not shipped, connected to, or treated as an owned gateway. Backend importer rejects these secrets/crypto settings |
| Original google-services (1).json | usa-favor, only com.usa.favor | Never used for either build |
| New google-services.json | Contains com.b5studios.usafavor and obsolete com.usa.favor | Admin registration selected |
| New google-services (1).json | Contains admin, obsolete registration, and com.usafavor.client | Client registration selected |
| Design reference | Dark navy/purple, gold branding, US shield, skyline, glass cards, circular power and bottom navigation | Recreated with native Compose and original editable vectors |

The two installed JSON copies contain only their corresponding current registration. Project metadata and the matching Firebase keys/app IDs are preserved. No Firebase key was deleted or changed in the Firebase project. Original attachments are unchanged. Generated Google Services resources are compared with the selected registration by `tools/verify-firebase.py` without printing API keys.

The profile's CA is ISRG Root X1 (2015–2035); its dummy client certificate is VPNGateClientCert (2013–2037). Certificate dates alone establish neither gateway ownership nor operational/security readiness. Its shared private key is excluded from this project and APKs.
