// Isolated handler tests with a serial transaction test double; not a substitute for Firestore Emulator tests.
const {test}=require('node:test');const a=require('node:assert/strict');const Module=require('node:module');
const data=new Map();let queue=Promise.resolve();
function ref(path){return {path,id:path.split('/').at(-1),async get(){return snap(path)},async create(v){if(data.has(path))throw Error('exists');data.set(path,structuredClone(v));},async set(v){data.set(path,structuredClone(v));},async update(v){data.set(path,{...data.get(path),...v});},async delete(){data.delete(path);}}}
function snap(path){return {id:path.split('/').at(-1),exists:data.has(path),ref:ref(path),data:()=>structuredClone(data.get(path))};}
function collection(path){return {doc(id='new-'+Math.random().toString(36).slice(2)){return ref(path+'/'+id)},async add(v){await this.doc().set(v)},where(k,op,v){return {async get(){const docs=[...data.keys()].filter(x=>x.startsWith(path+'/')&&data.get(x)[k]===v).map(snap);return {docs,forEach:f=>docs.forEach(f)}}}}};}
const db={doc:ref,collection,runTransaction(fn){const next=queue.then(async()=>{const writes=[];const tx={get:r=>r.get(),set:(r,v)=>writes.push(()=>r.set(v)),create:(r,v)=>writes.push(()=>r.create(v)),update:(r,v)=>writes.push(()=>r.update(v)),delete:r=>writes.push(()=>r.delete())};const result=await fn(tx);for(const w of writes)await w();return result;});queue=next.catch(()=>{});return next;},batch(){const writes=[];return {update:(r,v)=>writes.push(()=>r.update(v)),async commit(){for(const w of writes)await w()}}}};
const load=Module._load;
Module._load=function(id,...args){if(id==='firebase-admin/app')return {initializeApp(){}};if(id==='firebase-admin/firestore')return {getFirestore:()=>db,FieldValue:{serverTimestamp:()=>new Date()}};return load.call(this,id,...args)};
const api=require('../index');Module._load=load;
const request=(uid,details,admin=false)=>({auth:uid?{uid,token:{admin}}:null,data:details,rawRequest:{ip:'203.0.113.1'}});
test('privileged handlers reject missing or ordinary authentication',async()=>{for(const name of ['generateLicense','listLicenses','manageLicense','saveServer','saveConfiguration','saveSocial','importOvpn','manageServer']){await a.rejects(()=>api[name].run(request(null,{})),e=>e.code==='unauthenticated');await a.rejects(()=>api[name].run(request('customer',{})),e=>e.code==='permission-denied')}});
test('generation + simultaneous activation bind exactly one device; revoked license refresh denied',async()=>{
 const generated=await api.generateLicense.run(request('admin',{duration_days:30,activation_policy:'first_activation'},true));
 const results=await Promise.allSettled(['customer-a','customer-b'].map(id=>api.activateLicense.run(request(id,{code:generated.code}))));
 a.equal(results.filter(x=>x.status==='fulfilled').length,1);a.equal(results.filter(x=>x.status==='rejected').length,1);
 const license=data.get('licenses/'+generated.id);a.equal(license.device_binding_id,'customer-a');a.equal(license.expires_at-license.activated_at,30*86400000);
 await api.manageLicense.run(request('admin',{id:generated.id,action:'revoke'},true));await a.rejects(()=>api.licenseInfo.run(request('customer-a',{})));
});
test('invalid imports never create a server',async()=>{const before=[...data.keys()].filter(k=>k.startsWith('servers/')).length;await a.rejects(()=>api.importOvpn.run(request('admin',{text:'<key>secret</key>'},true)));a.equal([...data.keys()].filter(k=>k.startsWith('servers/')).length,before)});
test('remote settings reject executable or unknown fields',async()=>{await a.rejects(()=>api.saveConfiguration.run(request('admin',{settings:{javascript:'evil'}},true)));await a.rejects(()=>api.saveSocial.run(request('admin',{items:[{name:'bad',url:'javascript:alert(1)',icon:'custom',enabled:true}]},true)));});
test('customer cannot end another customer session',async()=>{data.set('sessions/other',{uid:'customer-a',license_id:'x'});await a.rejects(()=>api.endSession.run(request('customer-b',{session_id:'other'})));a.equal(data.get('sessions/other').revoked,undefined);});
