"use strict";
// Separate, opt-in serverless bridge. Spark licensing remains independent of Functions.
const {onCall,onRequest,HttpsError}=require("firebase-functions/v2/https");
const {defineSecret}=require("firebase-functions/params");
const {initializeApp}=require("firebase-admin/app");
const {getFirestore,Timestamp,FieldValue}=require("firebase-admin/firestore");
const {randomBytes}=require("node:crypto");
const {hash,authorize,secretMatches}=require("./policy");
initializeApp();const db=getFirestore();
const callbackSecret=defineSecret("VPN_PROVIDER_CALLBACK_SECRET");
async function check(transaction,uid,locationId) {
  const binding=(await transaction.get(db.doc(`bindings/${uid}`))).data();
  if(!binding?.license_id||!/^[a-f0-9]{64}$/.test(binding.license_id))throw Error("LICENSE_INACTIVE");
  const license=(await transaction.get(db.doc(`licenses/${binding.license_id}`))).data();
  const provider=(await transaction.get(db.doc("configuration/vpn_provider"))).data();
  const settings=(await transaction.get(db.doc("configuration/client"))).data();
  return authorize({uid,binding,license,provider,settings,locationId});
}
exports.issueProviderGrant=onCall({enforceAppCheck:true,maxInstances:10},async request=>{
  if(!request.auth)throw new HttpsError("unauthenticated","Activate your license first.");
  const locationId=request.data?.location_id;
  if(typeof locationId!=="string"||!/^[A-Za-z0-9_-]{1,128}$/.test(locationId))throw new HttpsError("invalid-argument","Choose a supported USA location.");
  const uid=request.auth.uid,grant=randomBytes(32).toString("base64url");
  try {
    await db.runTransaction(async transaction=>{
      const policy=await check(transaction,uid,locationId);
      transaction.create(db.doc(`provider_grants/${hash(grant)}`),{uid,location_id:locationId,expires_at:Timestamp.fromMillis(policy.grantExpiresAt),license_expires_at:Timestamp.fromMillis(policy.expiresAt),created_at:FieldValue.serverTimestamp(),used:false});
    });
    return {grant};
  }catch(error){
    if(["LICENSE_INACTIVE","PROVIDER_NOT_CONNECTED","LOCATION_DISABLED","SERVICE_PAUSED"].includes(error.message))throw new HttpsError("permission-denied",error.message);
    throw new HttpsError("unavailable","Provider authorization is temporarily unavailable.");
  }
});
// Register this method and its Bearer secret with the provider's documented custom OAuth plugin.
// Confirm request/response mapping with the partner; this is not an invented partner API endpoint.
exports.validateProviderGrant=onRequest({secrets:[callbackSecret],maxInstances:10},async(req,res)=>{
  res.set("Cache-Control","no-store");
  if(req.method!=="POST")return res.status(405).json({result:"Not found"});
  const sharedSecret=callbackSecret.value();
  if(!sharedSecret)return res.status(503).json({result:"Not found"});
  if(!secretMatches(req.get("Authorization"),`Bearer ${sharedSecret}`))return res.status(401).json({result:"Not found"});
  const grant=req.body?.access_token;
  if(typeof grant!=="string"||!/^[A-Za-z0-9_-]{43}$/.test(grant))return res.status(401).json({result:"Not found"});
  try {
    const result=await db.runTransaction(async transaction=>{
      const ref=db.doc(`provider_grants/${hash(grant)}`),record=(await transaction.get(ref)).data();
      if(!record||record.used||record.expires_at.toMillis()<=Date.now())throw Error("INVALID_GRANT");
      const policy=await check(transaction,record.uid,record.location_id);
      transaction.update(ref,{used:true});
      return {result:"OK",user:{uid:record.uid},expires_at:Math.floor(policy.expiresAt/1000),location_id:record.location_id};
    });
    return res.status(200).json(result);
  }catch(_error){return res.status(401).json({result:"Not found"});}
});
