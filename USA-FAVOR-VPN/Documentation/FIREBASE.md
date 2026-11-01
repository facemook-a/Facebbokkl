# Firebase project usa-favor

## Registrations and build

- Admin: `Admin-App/app/google-services.json` → `com.b5studios.usafavor`.
- Client: `Client-App/app/google-services.json` → `com.usafavor.client`.
- Run `python3 tools/verify-firebase.py`. The old `com.usa.favor` registration is excluded from both local JSON copies and generated app resources.
- Android API keys identify the project; they are not administrator/service-account credentials. Do not delete existing valid Firebase keys. Check API restrictions and Android package/signing certificate restrictions if Auth/App Check requests fail.

## Owner setup (not performed in this task)

1. Enable billing (Blaze), Firestore in Native mode, Firebase Authentication Email/Password and Anonymous providers. Do not enable customer self-registration in the Admin UI; there is no such control.
2. Create the administrator email/password account in Firebase Console. Use Application Default Credentials for a project owner on a trusted workstation, then run from `Firebase-Backend/functions`:
   ```sh
   GOOGLE_CLOUD_PROJECT=usa-favor node grant-admin.js FIREBASE_AUTH_UID
   ```
   The UID is not an email. This grants the server-side `admin` custom claim. Sign out/in after granting it. Do not put service-account JSON in an app, this repository, or chat.
3. Configure App Check for both Android registrations. Register signing fingerprints. Debug APKs use Firebase's Debug App Check provider; register each installation's debug token securely in Firebase Console. Do not distribute those debug tokens. Production uses Play Integrity. Configure its accepted verdicts for the actual distribution channel (including legitimate sideloading if used), then test on physical devices. No application-level bypass is implemented.
4. Enable Cloud Functions, Cloud Build, Artifact Registry, Secret Manager and Cloud Scheduler APIs. Deploy using an authorized Firebase CLI session, Node 22 and the correct project:
   ```sh
   cd Firebase-Backend/functions
   npm ci --ignore-scripts
   npm test
   cd ..
   firebase deploy --project usa-favor --only firestore,functions
   ```
   Use a supported current Firebase CLI. The deployed function runtime is Node.js 22 and the region is us-central1. Check deployed endpoints and logs, with credential redaction.
5. Give the deployed Functions runtime service account Firestore access and Secret Manager `secretAccessor` restricted to secrets prefixed `usa-favor-gateway-`. Use the service account actually displayed on the deployed v2 functions, not an assumed account. Audit project IAM separately. No service-account key is needed in Android.
6. Apply Firestore TTL for `rate_limits.expires_at` using the supplied field override. Optional retention policies for expired sessions/audit events must respect support/privacy requirements.

## Register an owned gateway

Save a disabled server registration in Admin → VPN Control Center. Copy its assigned gateway ID. For that VPS, securely generate a new random control key (`secrets.token_urlsafe(32)`), store it in the root-only gateway configuration, and create Secret Manager secret `usa-favor-gateway-ID` with that same value. Grant the Functions runtime read access. The secret payload is the key alone, not JSON. Use secure terminal/file inputs; never send the key in WhatsApp/chat or put it in Firestore.

The backend reads latest secret versions with a 60-second cache. To add another server, deploy another gateway and create its own secret, then register/test/publish it. No Client APK rebuild or Functions code deployment is needed for this operation. Hostname/IP/DNS/TLS changes still require actual DNS, certificates, routing and listener configuration at the gateway; editing a database record alone cannot provision them.

For initial deployment with no gateways, licenses/settings can be managed but the Client correctly shows no operational server. Do not publish a fabricated IP or claim connectivity until the acceptance test passes.

## Live acceptance

Sign in as an authorized admin. Confirm ordinary/anonymous accounts cannot invoke admin functions or read Firestore. Create generation/first-activation licenses. Test activation, second-device denial, expiry, reset, revoke, extension and deletion. Inspect the audit records. Publish settings/social changes and confirm Client refresh within one minute. Run gateway acceptance from GATEWAY.md, then exercise disconnect/reconnect, revocation and expiry while connected.

No deployed Firebase project behavior or API-key authorization was verified in this task. Correct configuration selection and compilation were verified. File contents alone do not prove provider enablement, deployed Functions, IAM or App Check readiness.
