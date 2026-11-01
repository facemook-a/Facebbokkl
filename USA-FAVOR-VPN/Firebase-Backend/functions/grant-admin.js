'use strict';
// Run locally with Application Default Credentials belonging to an authorized project owner.
// Never bundle this script or credentials in an APK.
const {initializeApp,applicationDefault}=require('firebase-admin/app');const {getAuth}=require('firebase-admin/auth');
const uid=process.argv[2];if(!uid)throw Error('Usage: GOOGLE_CLOUD_PROJECT=usa-favor node grant-admin.js FIREBASE_AUTH_UID');
initializeApp({credential:applicationDefault(),projectId:'usa-favor'});
(async()=>{const a=getAuth(),u=await a.getUser(uid);await a.setCustomUserClaims(uid,{...u.customClaims,admin:true});console.log('Administrator role set. Sign out and sign back in.');})().catch(()=>{console.error('Admin role update failed. Check project owner ADC permissions and UID.');process.exitCode=1;});
