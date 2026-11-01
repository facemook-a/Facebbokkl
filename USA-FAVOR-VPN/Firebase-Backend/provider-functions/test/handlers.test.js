const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const express=require('express');
const http=require('node:http');
// Synthetic test-only fixture, never a real partner credential.
process.env.VPN_PROVIDER_CALLBACK_SECRET='synthetic-provider-callback-test';
const bridge=require('../index');
let server,url;
before(async()=>{const app=express();app.use(express.json());app.all('/',bridge.validateProviderGrant);server=http.createServer(app);await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));url=`http://127.0.0.1:${server.address().port}/`;});
after(async()=>{await new Promise(resolve=>server.close(resolve));delete process.env.VPN_PROVIDER_CALLBACK_SECRET;});
test('real callable rejects unauthenticated and malformed location requests before database access',async()=>{await assert.rejects(bridge.issueProviderGrant.run({data:{location_id:'us-east'}}),e=>e.code==='unauthenticated');for(const location_id of ['',null,'../other','a'.repeat(129)])await assert.rejects(bridge.issueProviderGrant.run({auth:{uid:'one'},data:{location_id}}),e=>e.code==='invalid-argument');});
test('real provider callback rejects wrong method and missing authentication',async()=>{assert.equal((await fetch(url)).status,405);const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'});assert.equal(r.status,401);assert.deepEqual(await r.json(),{result:'Not found'});assert.equal(r.headers.get('cache-control'),'no-store');});
test('real provider callback rejects malformed grants even with valid callback authentication',async()=>{for(const access_token of [null,'short','/'.repeat(43)]){const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer synthetic-provider-callback-test'},body:JSON.stringify({access_token})});assert.equal(r.status,401);assert.deepEqual(await r.json(),{result:'Not found'});}});
test('real provider callback refuses service when callback secret is not configured',async()=>{process.env.VPN_PROVIDER_CALLBACK_SECRET='';try{const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'});assert.equal(r.status,503);}finally{process.env.VPN_PROVIDER_CALLBACK_SECRET='synthetic-provider-callback-test';}});
