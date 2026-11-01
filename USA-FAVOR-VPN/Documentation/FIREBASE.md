# Firebase usa-favor — Spark default

The default Android path uses Firebase Authentication and direct Firestore transactions protected by `firestore.rules`. It does **not** call or deploy Cloud Functions, Scheduler, Secret Manager, TTL or Cloud Storage. Firebase Spark quotas apply; see FREE-SETUP.md for phone-friendly instructions.

1. Create a **Standard / Native, (default)** Firestore database in project usa-favor, in production mode. Publish the complete supplied rules in Console. Alternatively an authorized Firebase CLI can run `firebase deploy --project usa-favor --only firestore` from Firebase-Backend. `firebase.json` deliberately has no Functions section and no TTL field override.
2. Enable Auth Email/Password for the private administrator and Anonymous for Client installation identities. No customer email/password UI is present.
3. Your administrator must have the explicit Firebase Auth `admin:true` custom claim. Existing claims remain valid. A project owner can use the retained `functions/grant-admin.js` helper on a trusted Cloud Shell with its normal Google credentials; this helper does not deploy or require Functions. Install its locked Node dependencies first if needed, then run `GOOGLE_CLOUD_PROJECT=usa-favor node grant-admin.js FIREBASE_AUTH_UID`. Sign out/in after setting the claim. Never copy service-account keys into Android, this repository or chat.
4. Admin JSON belongs in Admin-App/app/google-services.json (com.b5studios.usafavor); Client JSON belongs in Client-App/app/google-services.json (com.usafavor.client). `tools/verify-firebase.py` validates project/package/app-ID selection. The obsolete com.usa.favor registration is excluded. Do not delete valid Firebase API keys; check Android package/signing/API restrictions for authentication failures.
5. App Check SDK initialization is installed. Debug uses Firebase's Debug provider; release uses Play Integrity. Before enforcing App Check at the Firestore service or owned gateway, register the actual signing fingerprints/distribution configuration and test real installations. Already-enforced App Check still requires valid attestation/debug registration; this release does not bypass service enforcement. The gateway defaults to nonenforcement for initial staging in its root-only config, independently of license checks.

## Rules and schema

Schema version 3 uses private licenses, uid bindings, immutable second-installation conflicts, installation-reported presence, private server/profile inventory, public metadata restricted to valid bound licenses, admin configuration and audit events. IDs for known-code lookup use SHA-256. Inventory enumeration is admin-only. Creation/activation timestamps are server transforms enforced against request.time. Effective expiry is explicit `expires_at`, or generation/activation timestamp plus duration; Client cannot choose or extend it. Activation and binding commit atomically. Reset preserves the paid activation anchor/expiry.

Code possession permits a targeted lookup of an unbound code, not collection listing. After binding only the owning uid/admin may read it. Conflict creation checks that a different uid really owns the specified license. The device-model/presence value is self-reported and cannot prove physical device identity. A reinstall/new uid requires admin reset. Revocation, deletion and binding reset invalidate authorization; the owned gateway enforces this at its next bounded authorization check.

There is no custom IP rate-limit server on Spark. Codes contain 40 bits of randomness, and Spark quotas/App Check are relevant limits. Protect project access and monitor quota/abuse. No unlimited-free-scale claim is made.

The retained `functions/` and `firebase.legacy-functions.json` are legacy material, not the active runtime and not a supported schema3 deployment. Do not deploy them against current licenses. Existing production data needs a reviewed migration/export, not silent conversion.

## Acceptance still required

After publishing rules, sign in as admin, generate and activate a test code, reject a second installation, revoke/reset/extend it and check remote settings. Observe real records, denied requests and quotas. Rules have passed the emulator integration tests, but this workspace did not authenticate to the user's deployed Firebase database or publish rules on their behalf.
