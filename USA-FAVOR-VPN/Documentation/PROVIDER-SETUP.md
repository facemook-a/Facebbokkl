# Managed VPN provider — 1.2.0-provider

The normal apps no longer ask the administrator to manage a VPS, SSH, firewall, issuing CA, or OpenVPN profiles. Existing branding, package IDs, licensing, announcements, social links and dashboard customization remain. The Client now has green illuminated controls and optional app selection.

## Current service state

There are **no authorized partner credentials or supported SDK artifacts supplied in this workspace**. The normal APKs compile and run in provider-not-connected mode. They cannot create a tunnel and never display a simulated connected state or invented American IP. Existing OpenVPN source, gateway source and old downloads are retained for provenance; these are not used by the new default Client.

The chosen documented integration route is the **Aura Partner Unified VPN SDK for Android**. Official vendor-owned references:

- https://github.com/AnchorFreePartner/documentation/tree/master/sdk/vpn-sdk-for-android
- https://github.com/AnchorFreePartner/documentation/blob/master/sdk/vpn-sdk-for-android/usage/authentication.md
- https://github.com/AnchorFreePartner/documentation/blob/master/sdk/vpn-sdk-for-android/usage/vpn-interface.md
- https://github.com/AnchorFreePartner/documentation/blob/master/console-details/project-settings/user-authentification-methods/auth-plugin-requirements.md

These documents establish an SDK/custom-OAuth integration route, **not permission to resell service**. The old public SDK/sample repository links currently return repository-not-found. Obtain a current supported SDK, integration details, USA catalog, pricing and explicit distribution/resale permission from the partner. Do not use an unverified mirror or an old sample's carrier key.

## One-time partner integration

An authorized integration maintainer completes this once; normal Admin users only toggle locations.

1. Obtain a pinned SDK coordinate and its authorized HTTPS Maven repository. Build with `-PauraSdkCoordinate=GROUP:ARTIFACT:PINNED_VERSION` and, if needed, `-PauraSdkRepository=https://PARTNER_REPOSITORY`. These are public selectors, not master credentials. The source adapter is `Client-App/app/src/aura/java/com/usafavor/client/AuraProvider.kt`. It follows the documented UnifiedSDK API. **It is not compiled or certified without that artifact; reconcile its API against the supplied supported version before enabling live service.** Retain vendor R8/service declarations required by that version.
2. Deploy only the separate `vpn-provider` Firebase Functions codebase with `firebase deploy --config firebase.provider.json --project usa-favor --only functions:vpn-provider`. Install its locked dependencies first. These managed Functions require an appropriate Firebase billing plan; **Spark remains sufficient for licensing alone**. No Functions deployment or billing change is performed automatically.
3. Set `VPN_PROVIDER_CALLBACK_SECRET` in Firebase Secret Manager using the Firebase CLI's secure prompt. Share it with the authorized provider through their secure onboarding process. Never include it, master API credentials, or service-account JSON in Android configuration. Register the `validateProviderGrant` endpoint as a provider custom OAuth plugin: `POST`, Bearer callback authentication, JSON `access_token`; map `user.uid` from its documented GetUserInfo response. Ask the provider to confirm request mapping and support for stable string UIDs and single-use grants/retries.
4. Verify the partner **enforces a single device, paid expiry and revocation for active sessions on its own infrastructure**. `expires_at` and `location_id` response fields are integration metadata; the vendor's documented basic OAuth response alone does not establish enforcement. Configure the partner's agreed entitlement/session mechanism to consume those values and recheck/revoke sessions. This contract-specific piece cannot be invented from generic SDK docs. Test expiry, reset, revocation, process death, reconnection and disabled-location behavior against the real partner before declaring it verified.
5. A trusted integration using Firebase Admin SDK writes `configuration/vpn_provider` with the schema below. Fetch the catalog from the partner's authorized API/export; use the SDK's actual location IDs, not invented server addresses. Keep readiness flags false until the above acceptance passes. Android admins cannot change these flags, catalog or provider identity.

```json
{
  "provider": "aura",
  "carrier_id": "PARTNER_PUBLIC_PROJECT_ID",
  "base_url": "https://PARTNER_APPROVED_BACKEND",
  "integration_verified": false,
  "expiry_enforcement_verified": false,
  "per_app_supported": false,
  "supported_us_location_ids": [],
  "locations": [],
  "enabled_locations": []
}
```

Each real catalog item contains `id`, `name`, `city`, `country_code` (`US` for USA). Publish at most 100 supported USA IDs. `per_app_supported` is true only after confirming AppPolicy support in the contracted SDK. SDK status plus an Android VPN network is necessary before the Client shows Connected; no IP or traffic measurements are fabricated. The app includes its control traffic in selected-app routing. App selection lists launchable installed apps without QUERY_ALL_PACKAGES.

The serverless bridge issues random single-use 60-second grants after reading the current binding, license, provider readiness and enabled location in a Firestore transaction. Only SHA-256 hashes are stored; consumption rechecks authorization atomically. Grants never outlive the license. Configure Firestore TTL for grant `expires_at` if desired (billing/TTL behavior depends on Firebase); expiry checks do not depend on cleanup. Configure callable App Check enforcement and real signed/debug installations before testing. In-process checks and Firestore listeners provide additional disconnection, but are not a substitute for provider enforcement after process termination. Repeated failures during admission fail closed.

## Everyday experience

Admin: Login → License studio → Generate code / Manage licenses → VPN Connection Settings → toggle supported USA locations.

Client: Activate code once → select apps if supported (or keep all apps) → select an enabled USA location → connect using Android's VPN permission dialog. A later launch restores the same Firebase installation identity and license until expiry; reinstalling requires an Admin binding reset. Existing installation-binding limits still apply on rooted/cloned devices.

## Firebase permissions

Publish the current canonical rules in `Firebase-Backend/firestore.rules`; the rules copied by the Admin app are identical. Existing rules without `configuration/vpn_provider` access will deny the new Client configuration read. This release adds that **licensed read** while keeping provider grants private and catalog/readiness writes server-only. It does not open the database or self-grant admin rights.

A concrete preexisting Client bug caught **all** `licenseInfo` exceptions as if there were no license, silently concealing permission/App Check/network errors. The Client now suppresses only the explicit missing-binding result and reports actual Firebase failures. Publishing rules, an admin:true claim, the correct (default) database and App Check registration are still required. This workspace has public Android registrations but no authenticated Firebase administrator runtime, so live rule publication and deployed customer activation are not verified here.
