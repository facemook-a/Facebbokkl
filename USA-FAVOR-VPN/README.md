# USA FAVOR VPN — powered by B5 STUDIOS

Two native Kotlin/Jetpack Compose Android applications, Firebase backend, and a deployable Ubuntu OpenVPN gateway. No store, advertising, cart or in-app payments. Licenses are generated in the private Admin app and can be copied/shared manually, including WhatsApp.

## Project

| Folder | Contents |
|---|---|
| Admin-App/app | B5 STUDIOS ADMIN · com.b5studios.usafavor |
| Client-App/app | USA FAVOR VPN · com.usafavor.client |
| Firebase-Backend | Protected Functions, private Firestore schema/rules, rate limits, audits and tests |
| VPN-Gateway | OpenVPN/PKI, authorization controller, management, firewall/systemd/nginx deployment |
| Assets | Original editable logos/shield/skyline vectors and visual system |
| Documentation | Build, Firebase, architecture, security, deployment and validation |
| Shared | Compose components, backend repository, Keystore metadata |
| OpenVPN-Core | Vendored OpenVPN for Android v0.7.69 and pinned native dependencies |

## Build and operate

1. Read [BUILD.md](Documentation/BUILD.md), then run `./tools/build.sh` (or `./tools/cloud-build.sh` in this prepared workspace).
2. Complete owner setup and backend deployment in [FIREBASE.md](Documentation/FIREBASE.md).
3. Provision a real US-hosted VPS and follow [GATEWAY.md](Documentation/GATEWAY.md). Register its secret securely, test and publish it from VPN Control Center.
4. Generate a license in Admin, activate it on a device, approve Android VPN permission and perform the documented live acceptance. Remote server inventory/settings/social changes do not require ordinary Client APK rebuilds.

The two supplied current Firebase registrations are installed in the correct app folders. The obsolete com.usa.favor registration and uploaded shared-key VPN profile are excluded. See [INPUT-REVIEW.md](Documentation/INPUT-REVIEW.md).

The source implements Auth/roles, subscription policies, device binding, license search/pagination and management, server registration/import/verification/configuration/publication, settings/social management, native client navigation, tips, real engine states/traffic and VPN-bound public-IP checks. No statistics or Connected state are fabricated. The native design follows the reference's navy/purple/gold/US shield/skyline/glass direction; no static HTML demo is used.

See [VERIFICATION.md](Documentation/VERIFICATION.md) for actual build/test outcomes and [SECURITY.md](Documentation/SECURITY.md) for dependencies and trust boundaries. Debug APK compilation is not proof of a deployed backend or working VPN. This task did not have an operational VPS, deployment authorization/session, production signing keystore or attached Android device. These external deployment/testing steps remain necessary before selling access. Six moderate transitive npm findings remain documented; no high/critical findings were reported in that audit.

OpenVPN's corresponding source and license notices are included. Read `OpenVPN-Core/upstream-doc/LICENSE.txt` before distributing derivative binaries.
