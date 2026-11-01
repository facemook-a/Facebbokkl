'use strict';
const {onCall,HttpsError,onRequest}=require('firebase-functions/v2/https');
const {onSchedule}=require('firebase-functions/v2/scheduler');
const {SecretManagerServiceClient}=require('@google-cloud/secret-manager');
const {initializeApp}=require('firebase-admin/app');
const {getFirestore,FieldValue}=require('firebase-admin/firestore');
const {randomBytes}=require('node:crypto');
const https=require('node:https');const dns=require('node:dns').promises;
const D=require('./domain');initializeApp();const db=getFirestore();
const secretManager=new SecretManagerServiceClient();const keyCache=new Map();
const options={region:'us-central1',enforceAppCheck:true,maxInstances:20};
function uid(r){if(!r.auth)throw new HttpsError('unauthenticated','Authentication required');return r.auth.uid;}
function admin(r){uid(r);if(r.auth.token.admin!==true)throw new HttpsError('permission-denied','Administrator access required');}
function fail(e){if(e instanceof HttpsError)throw e;console.error('Operation failed',e.code||e.name);throw new HttpsError('failed-precondition',e.message?.slice(0,180)||'Operation failed');}
async function limit(r,action,max){
 const id=D.hash(uid(r)+':'+action+':'+Math.floor(Date.now()/60000));const ref=db.collection('rate_limits').doc(id);
 await db.runTransaction(async t=>{const ipref=db.collection('rate_limits').doc(D.hash(String(r.rawRequest?.ip||'unknown')+':'+action+':'+Math.floor(Date.now()/60000)));const [s,ip]=await Promise.all([t.get(ref),t.get(ipref)]);const n=s.data()?.n||0;if(n>=max||(ip.data()?.n||0)>=max*10)throw new HttpsError('resource-exhausted','Please try again in one minute');t.set(ref,{n:n+1,expires_at:new Date(Date.now()+120000)});t.set(ipref,{n:(ip.data()?.n||0)+1,expires_at:new Date(Date.now()+120000)});});
}
function callable(fn,isAdmin=false,max=30){return onCall(options,async r=>{try{uid(r);if(isAdmin)admin(r);await limit(r,fn.name||'api',max);return await fn(r);}catch(e){fail(e);}});}
async function audit(r,action,target){await db.collection('audit').add({actor:uid(r),action,target,at:FieldValue.serverTimestamp()});}
async function bound(uid){const b=await db.doc('bindings/'+uid).get();if(!b.exists)throw Error('Activate a license first');const ref=db.doc('licenses/'+b.data().license_id);const snap=await ref.get();const l=snap.data();if(!D.usable(l)||l.device_binding_id!==uid)throw Error('License unavailable, expired or revoked');return {ref,l,id:snap.id};}
async function keyFor(id){if(!/^[\w-]{1,80}$/.test(id))throw Error('Invalid gateway ID');const cached=keyCache.get(id);if(cached&&cached.until>Date.now())return cached.key;const [result]=await secretManager.accessSecretVersion({name:'projects/usa-favor/secrets/usa-favor-gateway-'+id+'/versions/latest'});const key=Buffer.from(result.payload.data).toString('utf8').trim();if(!/^[A-Za-z0-9_-]{43,128}$/.test(key))throw Error('Gateway management key is not provisioned');keyCache.set(id,{key,until:Date.now()+60000});return key;}
async function gateway(id,s,path,payload={}){
 if(!D.publicHost(s.host))throw Error('Gateway host is not public');
 const addresses=await dns.lookup(s.host,{all:true});if(!addresses.length||addresses.some(x=>x.family!==4||!D.publicHost(x.address)))throw Error('Gateway DNS must resolve exclusively to public IPv4');
 const body=JSON.stringify(payload);const address=addresses[0].address;const managementKey=await keyFor(id);
 return await new Promise((resolve,reject)=>{const req=https.request({hostname:s.host,port:443,path:'/control/'+path,method:'POST',servername:s.host,lookup:(_h,_o,cb)=>cb(null,address,4),timeout:10000,headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(body),'Authorization':'Bearer '+managementKey}},res=>{let data='';res.on('data',x=>{data+=x;if(data.length>131072)req.destroy(Error('Gateway response too large'));});res.on('end',()=>{try{if(res.statusCode!==200)throw Error('Gateway rejected request');resolve(JSON.parse(data));}catch(e){reject(e);}});});req.on('error',reject);req.on('timeout',()=>req.destroy(Error('Gateway timed out')));req.end(body);});
}
exports.generateLicense=callable(async function generateLicense(r){
 const days=D.duration(Number(r.data.duration_days));const policy=r.data.activation_policy;if(!['generation','first_activation'].includes(policy))throw Error('Invalid activation policy');
 const code=D.code(),id=D.hash(code),now=Date.now();
 await db.doc('licenses/'+id).create({code,is_active:true,status:'unused',created_at:now,created_by:uid(r),activated_at:null,expires_at:policy==='generation'?now+days*86400000:null,duration_days:days,activation_policy:policy,device_binding_id:null,revoked_at:null,updated_at:now});await audit(r,'license.create',id);return {code,id};
},true);
exports.activateLicense=callable(async function activateLicense(r){
 const code=String(r.data.code||'').trim().toUpperCase();if(!/^USA-[A-Z2-9]{4}-[A-Z2-9]{4}$/.test(code))throw Error('Enter USA-XXXX-XXXX');
 const id=D.hash(code),ref=db.doc('licenses/'+id),binding=db.doc('bindings/'+uid(r));let license;
 await db.runTransaction(async t=>{const [s,b]=await Promise.all([t.get(ref),t.get(binding)]);if(!s.exists)throw Error('License not found');if(b.exists&&b.data().license_id!==id){const old=await t.get(db.doc('licenses/'+b.data().license_id));if(D.usable(old.data()))throw Error('This device already has an active license');}license=D.activate(s.data(),uid(r));t.set(ref,license);t.set(binding,{license_id:id,at:Date.now()});});
 return {masked:'USA-••••-'+code.slice(-4),activated_at:license.activated_at,expires_at:license.expires_at,status:'active'};
},false,5);
exports.licenseInfo=callable(async function licenseInfo(r){const {l}=await bound(uid(r));return {masked:'USA-••••-'+l.code.slice(-4),activated_at:l.activated_at,expires_at:l.expires_at,status:D.status(l)};});
exports.listLicenses=callable(async function listLicenses(r){
 if(r.data.search){const code=String(r.data.search).trim().toUpperCase();if(!/^USA-[A-Z2-9]{4}-[A-Z2-9]{4}$/.test(code))throw Error('Use a full license code to search the entire inventory');const doc=await db.doc('licenses/'+D.hash(code)).get();return {items:doc.exists?[{id:doc.id,...doc.data(),status:D.status(doc.data())}]:[],next_cursor:null};}
 let query=db.collection('licenses').orderBy('created_at','desc').limit(200);if(r.data.cursor){if(!/^[a-f0-9]{64}$/.test(r.data.cursor))throw Error('Invalid cursor');const last=await db.doc('licenses/'+r.data.cursor).get();if(!last.exists)throw Error('Cursor changed; refresh the list');query=query.startAfter(last);}
 const q=await query.get();return {items:q.docs.map(s=>({id:s.id,...s.data(),status:D.status(s.data())})),next_cursor:q.size===200?q.docs.at(-1).id:null};
},true);
exports.manageLicense=callable(async function manageLicense(r){
 const {id,action}=r.data;if(!/^[a-f0-9]{64}$/.test(id))throw Error('Invalid license identifier');const ref=db.doc('licenses/'+id);
 await db.runTransaction(async t=>{const s=await t.get(ref);if(!s.exists)throw Error('License not found');const l=s.data(),now=Date.now();const b=l.device_binding_id?db.doc('bindings/'+l.device_binding_id):null;
 if(action==='delete'){t.delete(ref);if(b)t.delete(b);return;}
 let patch={updated_at:now};
 if(action==='revoke')patch={...patch,is_active:false,status:'revoked',revoked_at:now};
 else if(action==='reactivate'){if(l.expires_at&&l.expires_at<=now)throw Error('Extend the expired license first');patch={...patch,is_active:true,status:l.activated_at?'active':'unused',revoked_at:null};}
 else if(action==='extend')patch={...patch,expires_at:Math.max(now,l.expires_at||now)+D.duration(Number(r.data.days))*86400000};
 else if(action==='reset'){patch={...patch,device_binding_id:null};if(b)t.delete(b);}
 else throw Error('Unsupported action');t.update(ref,patch);
 });
 const sessions=await db.collection('sessions').where('license_id','==',id).get();for(let i=0;i<sessions.docs.length;i+=400){const batch=db.batch();sessions.docs.slice(i,i+400).forEach(s=>batch.update(s.ref,{revoked:true}));await batch.commit();}await audit(r,'license.'+action,id);return {ok:true};
},true);
exports.dashboard=callable(async function dashboard(){
 const [ls,ss,sessions]=await Promise.all([db.collection('licenses').get(),db.collection('servers').get(),db.collection('sessions').where('expires_at','>',Date.now()).get()]);
 const counts={total:ls.size,active:0,expired:0,revoked:0,unused:0,recent:0};ls.forEach(s=>{counts[D.status(s.data())]++;if(s.data().activated_at>Date.now()-7*86400000)counts.recent++;});
 return {...counts,servers:ss.size,offline:ss.docs.filter(s=>!s.data().health?.online||s.data().health?.checked_at<=Date.now()-120000).length,online:ss.docs.filter(s=>s.data().health?.checked_at>Date.now()-120000&&s.data().health?.online).length,sessions:sessions.docs.filter(s=>!s.data().revoked).length,active_sessions:ss.docs.reduce((n,s)=>n+(s.data().health?.checked_at>Date.now()-120000&&s.data().health?.online?Number(s.data().health.active_sessions||0):0),0),server_items:ss.docs.map(s=>({id:s.id,...s.data()}))};
},true);
exports.importOvpn=callable(async function importOvpn(r){return D.ovpnSummary(r.data.text);},true,10);
exports.saveServer=callable(async function saveServer(r){
 const id=r.data.id||db.collection('servers').doc().id;if(!/^[\w-]{1,80}$/.test(id))throw Error('Invalid server ID');const s=D.server(r.data.server);const ref=db.doc('servers/'+id),old=(await ref.get()).data();
 // Registration and draft edits never imply ownership or successful provisioning.
 await ref.set({...s,config_version:(old?.config_version||0)+1,verified:false,published:false,updated_at:Date.now()});await db.doc('public_servers/'+id).delete();if(['primary','backup'].includes(s.role)){const peers=await db.collection('servers').where('role','==',s.role).get();const b=db.batch();for(const p of peers.docs)if(p.id!==id){b.update(p.ref,{role:'standard'});const pub=await db.doc('public_servers/'+p.id).get();if(pub.exists)b.update(pub.ref,{role:'standard'});}await b.commit();}await audit(r,'server.save',id);return {id};
},true);
exports.manageServer=callable(async function manageServer(r){
 const {id,action}=r.data;if(!/^[\w-]{1,80}$/.test(id))throw Error('Invalid server');const ref=db.doc('servers/'+id),s=(await ref.get()).data();if(!s)throw Error('Unknown server');
 if(action==='remove'){await ref.delete();await db.doc('public_servers/'+id).delete();}
 else if(action==='disable'){await ref.update({enabled:false,published:false});await db.doc('public_servers/'+id).delete();}
 else if(['test','publish','rotate','configure'].includes(action)){
  if(action==='configure'){await gateway(id,s,'configure',{port:s.port,transport:s.transport,dns:s.dns,max_sessions:s.max_sessions});await ref.update({verified:false,published:false});await db.doc('public_servers/'+id).delete();await audit(r,'server.configure',id);return {ok:true};}
  if(action==='rotate'){const result=await gateway(id,s,'rotate');await audit(r,'server.rotate',id);return result;}
  const h=await gateway(id,s,'health');if(h.gateway_id!==id||h.protocol!=='openvpn'||!h.online)throw Error('Gateway identity or service verification failed');
  if(h.host!==s.host||h.port!==s.port||h.transport!==s.transport||JSON.stringify(h.dns)!==JSON.stringify(s.dns))throw Error('Gateway deployment differs from inventory; update the gateway deployment first');
  const health={...h,checked_at:Date.now()};await ref.update({health,last_success_at:Date.now(),verified:true,published:action==='publish'||s.published===true});if(action==='test'&&s.published)await db.doc('public_servers/'+id).update({health});
  if(action==='publish'){if(!s.enabled||s.maintenance)throw Error('Enable server and exit maintenance first');await db.doc('public_servers/'+id).set({...D.server(s),id,config_version:s.config_version,health});}
  await audit(r,'server.'+action,id);return {health};
 }else throw Error('Unsupported server action');await audit(r,'server.'+action,id);return {ok:true};
},true,10);
exports.getConfiguration=callable(async function getConfiguration(r){await bound(uid(r));const [servers,settings,social]=await Promise.all([db.collection('public_servers').get(),db.doc('configuration/client').get(),db.doc('configuration/social').get()]);return {servers:servers.docs.map(s=>({id:s.id,...s.data()})),settings:settings.data()||{},social:social.data()?.items||[]};});
exports.adminConfiguration=callable(async function adminConfiguration(){const [settings,social]=await Promise.all([db.doc('configuration/client').get(),db.doc('configuration/social').get()]);return {settings:settings.data()||{},social:social.data()?.items||[]};},true);
exports.saveConfiguration=callable(async function saveConfiguration(r){
 const allowed=['welcome','announcement','maintenance_message','support','privacy_url','terms_url','help','activation_popup','minimum_version','update_notice','default_server','server_order','maintenance','show_social','show_tips'];const v=r.data.settings||{};
 if(Object.keys(v).some(k=>!allowed.includes(k)))throw Error('Unsupported setting');for(const [k,val]of Object.entries(v)){if(['maintenance','show_social','show_tips'].includes(k)){if(typeof val!=='boolean')throw Error('Invalid maintenance');}else if(k==='minimum_version'){if(!Number.isInteger(val)||val<1)throw Error('Invalid version');}else if(typeof val!=='string'||val.length>4000)throw Error('Invalid setting');if(k.endsWith('_url')&&val&&!D.safeUrl(val))throw Error('HTTPS URL required');}
 await db.doc('configuration/client').set(v);await audit(r,'configuration.save','client');return {ok:true};
},true);
exports.saveSocial=callable(async function saveSocial(r){
 const items=r.data.items;if(!Array.isArray(items)||items.length>30)throw Error('Maximum 30 links');for(const x of items)if(!D.safeUrl(x.url)||typeof x.name!=='string'||x.name.length>80||!['whatsapp','telegram','instagram','tiktok','facebook','youtube','website','email','custom'].includes(x.icon)||typeof x.enabled!=='boolean')throw Error('Invalid social link');
 await db.doc('configuration/social').set({items:items.map((x,order)=>({name:x.name,url:x.url,icon:x.icon,enabled:x.enabled,order}))});await audit(r,'social.save','social');return {ok:true};
},true);
exports.startSession=callable(async function startSession(r){
 const {l,id:license_id}=await bound(uid(r));const sid=r.data.server_id;const s=(await db.doc('servers/'+sid).get()).data();const cfg=(await db.doc('configuration/client').get()).data()||{};
 if(cfg.maintenance||Number(r.data.version||0)<(cfg.minimum_version||1))throw Error('Service maintenance or app update required');
 if(!s?.published||!s.enabled||s.maintenance||!s.verified)throw Error('Server is unavailable');
 const token=randomBytes(32).toString('base64url'),session_id=db.collection('sessions').doc().id;const expires_at=Math.min(l.expires_at,Date.now()+3600000);
 const lock=db.doc('session_locks/'+license_id);
 await db.runTransaction(async t=>{const current=await t.get(lock);if(current.exists&&current.data().until>Date.now()){const oldRef=db.doc('sessions/'+current.data().session_id);const old=await t.get(oldRef);if(old.exists&&!old.data().revoked&&old.data().expires_at>Date.now()&&old.data().uid!==uid(r))throw Error('Another device has an active session');if(old.exists)t.update(oldRef,{revoked:true});}t.set(lock,{session_id,until:expires_at});t.create(db.doc('sessions/'+session_id),{uid:uid(r),license_id,server_id:sid,token_hash:D.hash(token),expires_at,created_at:Date.now(),revoked:false});});
 try{const result=await gateway(sid,s,'profile',{session_id,token});if(typeof result.profile!=='string'||result.profile.length>65536)throw Error('Invalid gateway profile');return {session_id,expires_at,profile:result.profile,egress_ip:result.egress_ip};}catch(e){await db.doc('sessions/'+session_id).update({revoked:true});await db.runTransaction(async t=>{const v=await t.get(lock);if(v.data()?.session_id===session_id)t.delete(lock);});throw e;}
},false,5);
exports.endSession=callable(async function endSession(r){const ref=db.doc('sessions/'+r.data.session_id),s=(await ref.get()).data();if(!s||s.uid!==uid(r))throw Error('Unknown session');await ref.update({revoked:true});const lock=db.doc('session_locks/'+s.license_id);await db.runTransaction(async t=>{const l=await t.get(lock);if(l.data()?.session_id===r.data.session_id)t.delete(lock);});return {ok:true};});
exports.gatewayAuthorize=onRequest({region:'us-central1',maxInstances:20},async(req,res)=>{
 try{
  if(req.method!=='POST')return res.sendStatus(405);const {gateway_id,session_id,token}=req.body||{};if(!/^[\w-]{1,80}$/.test(gateway_id)||!/^\w{1,80}$/.test(session_id))return res.sendStatus(403);
  if(!D.equal(req.headers.authorization,'Bearer '+await keyFor(gateway_id)))return res.sendStatus(403);
  const [sp,gp]=await Promise.all([db.doc('sessions/'+session_id).get(),db.doc('servers/'+gateway_id).get()]);const s=sp.data(),g=gp.data();if(!s||!g?.enabled||g.maintenance||!g.published||s.revoked||s.server_id!==gateway_id||s.expires_at<=Date.now()||!D.equal(s.token_hash,D.hash(String(token))))return res.sendStatus(403);
  const l=(await db.doc('licenses/'+s.license_id).get()).data();if(!D.usable(l)||l.device_binding_id!==s.uid)return res.sendStatus(403);res.json({authorized:true,expires_at:Math.min(s.expires_at,l.expires_at)});
 }catch{res.sendStatus(403);}
});
exports.healthSweep=onSchedule({schedule:'every 1 minutes',region:'us-central1'},async()=>{const all=await db.collection('servers').get();await Promise.allSettled(all.docs.map(async d=>{const s=d.data();try{const h=await gateway(d.id,s,'health');if(h.gateway_id!==d.id||!h.online)throw Error('Gateway identity mismatch');const health={...h,checked_at:Date.now()};await d.ref.update({health,last_success_at:Date.now()});if(s.published)await db.doc('public_servers/'+d.id).update({health});}catch{const health={online:false,checked_at:Date.now()};await d.ref.update({health});if(s.published)await db.doc('public_servers/'+d.id).update({health});}}));});

exports.listSessions=callable(async function listSessions(r){const id=r.data.license_id;if(!/^[a-f0-9]{64}$/.test(id))throw Error('Invalid license');const q=await db.collection('sessions').where('license_id','==',id).limit(100).get();return {items:q.docs.map(d=>({id:d.id,server_id:d.data().server_id,created_at:d.data().created_at,expires_at:d.data().expires_at,revoked:d.data().revoked}))};},true);
