# B5 STUDIOS Admin + USA VPN — 1.3.0-studio

## Everyday use

Client: enter an unused USA-XXXX-XXXX code → see successful activation → use the app without entering the code again until expiry. Every customer can enter a code on their own phone; the first installation atomically owns that code. A second installation is rejected. Signing out or reinstalling is not the normal renewal flow. Expiry, revocation and an explicit Admin binding reset remain server enforced. Disconnect/reconnect does not redeem a second license. Actual VPN still needs the authorized provider described in PROVIDER-SETUP.md.

Admin: sign in once as an authorized owner → later unlock the saved owner session using the phone's biometric or Android screen lock → manage codes, expiry, sections, announcements, social links and update notices. The biometric prompt unlocks an existing Firebase identity, not a new admin claim. A phone without a supported secure lock must use owner authentication. No private owner password is persisted by this code. Arabic/English selection is saved; Arabic uses RTL and bundled Cairo typography. Client stays English; launcher label is USA VPN.

A side menu links Licenses, VPN settings, Client sections / Code editor, App updates, Package library and social links. Normal management contains no Firebase Console, SSH, firewall or certificate controls. Initial hosted service deployment and owner authorization are still prerequisites; they are not silently bypassed by the app.

## Sections and tools

Add, edit, hide, remove and reorder up to 20 cards on Home or Settings. Button actions include opening a link, displaying the license/activity/settings, refreshing data, selecting apps, connect/disconnect, and app updates. Actions call existing native app flows with the same license, capability and Android permission checks.

A code tool has HTML, CSS and JavaScript fields, a title, description and button label. It opens in a non-exported Activity after current license authorization. The WebView is isolated: no JavaScriptInterface/native bridge, no provider/Firebase credentials, no network requests, no file/content access, no persistent web storage, no external navigation or child windows. A CSP permits inline code/styles and data images only. Code tools are intended for self-contained calculators and information tools. They cannot install native code, change VPN engines, access device permissions or implement a new provider SDK. Owner code can contain logic bugs; real-device WebView acceptance remains necessary.

Each code field is limited to 32 KiB; combined section content is capped at 512 KiB. A code tool needs HTML. Unknown native action names and unsafe links are rejected. License listener and periodic server checks remove tool access when authorization fails.

## Package library

Import a JSON file through Android's document picker, validate it, review its kind and publish it explicitly. Packages are data, not dex/APK/so modules. Supported kinds:

- sections: replaces the section list after the same editor validation.
- provider: saves private pending public partner connection details. It cannot set readiness, import secrets, install a vendor SDK or activate a provider.
- update: publishes an update manifest after validation.

Examples are in Packages/. The calculator is a self-contained working HTML/JS example. The provider example uses a reserved example.com address and is a TEMPLATE, not a working provider. No package auto-grants licenses or administrator rights.

## In-app updates

Admin publishes an HTTPS APK download URL, SHA-256, newer integer versionCode, minimum version, optional required flag and message. Upload the new APK to your authorized distribution location first; this app does not invent a hosting service or upload credentials. The manifest is read from configuration/app_update by signed installations before activation. It contains public update metadata only; clients cannot write it. Publish the current canonical Firestore rules once through an authorized service operator before testing on the live project.

Client downloads within the app into a private cache. It preserves TLS, permits only public HTTPS redirects, checks byte count/size (128 MiB limit), validates SHA-256, checks the exact client package and advertised newer version, and requires the existing installation's signing certificate before opening Android's installer. The FileProvider exposes only the verified-update cache. An update manifest change invalidates an old prepared file. Required notices block the normal app UI/connect flow; disabling or correcting the notice can be picked up by refresh/polling.

Android requires the user's installation confirmation and may require enabling installs from this source once. After replacement, Android offers Open; no silent installation or guaranteed forced self-restart is claimed. An in-place update retains Firebase installation identity/activation. A new signing key cannot update the old install. Existing production users must receive an APK signed by the original key; do not erase a licensed install merely to work around a mismatch.

REQUEST_INSTALL_PACKAGES is for this explicitly initiated APK distribution workflow. Distribution through an app store needs review of that store's updater/install policies. No physical-device installation, biometric sensor, WebView tool, live Firebase activation or real VPN tunnel was validated by local Compose rendering alone.
