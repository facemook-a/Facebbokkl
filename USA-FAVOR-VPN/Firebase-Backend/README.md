# Spark backend (active)

Use firebase.json and firestore.rules with a Standard Native (default) Firestore database. No Functions deployment, billing upgrade, TTL or Storage is part of the active Android v1.1.0-spark path. See ../Documentation/FREE-SETUP.md and FIREBASE.md.

rules-tests/ contains the official-emulator authorization integration tests. functions/ and firebase.legacy-functions.json retain the earlier Functions implementation only; it is not schema3-compatible and must not be deployed against the current database. grant-admin.js is a standalone owner helper; setting an Auth custom claim does not require deploying Functions.
