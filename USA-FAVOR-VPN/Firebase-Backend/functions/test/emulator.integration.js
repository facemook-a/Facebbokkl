// Optional real Firestore integration: requires the local demo emulator, never a production project.
const {test}=require('node:test');const a=require('node:assert/strict');
a.equal(process.env.GCLOUD_PROJECT,'demo-usa-favor','Only the demo emulator project is permitted');
a.match(process.env.FIRESTORE_EMULATOR_HOST||'',/^(127\.0\.0\.1|localhost):\d+$/,'A local Firestore Emulator is required');
const api=require('../index');const {getFirestore}=require('firebase-admin/firestore');const db=getFirestore();
const req=(uid,data,admin=false)=>({auth:{uid,token:{admin}},data,rawRequest:{ip:'127.0.0.1'}});
test('real Firestore activation transaction permits one installation, then revoke enforces denial',async()=>{
 const seed=Date.now().toString();const code=await api.generateLicense.run(req('admin-'+seed,{duration_days:1,activation_policy:'first_activation'},true));
 try{
  const responses=await Promise.allSettled(['one-','two-'].map(x=>api.activateLicense.run(req(x+seed,{code:code.code}))));a.equal(responses.filter(x=>x.status==='fulfilled').length,1);
  const l=(await db.doc('licenses/'+code.id).get()).data();a.ok(l.device_binding_id);a.equal(l.expires_at-l.activated_at,86400000);
  await api.manageLicense.run(req('admin-'+seed,{id:code.id,action:'revoke'},true));await a.rejects(()=>api.licenseInfo.run(req(l.device_binding_id,{})));
 }finally{await db.doc('licenses/'+code.id).delete();for(const p of ['one-','two-'])await db.doc('bindings/'+p+seed).delete();}
});
test('unauthenticated Firestore REST reads are denied by supplied rules',async()=>{
 const host=process.env.FIRESTORE_EMULATOR_HOST;const r=await fetch(`http://${host}/v1/projects/demo-usa-favor/databases/(default)/documents/licenses`);a.equal(r.status,403);
});
