'use strict';
const {randomInt, createHash, timingSafeEqual}=require('node:crypto');
const {isIP}=require('node:net');
const ALPHABET='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
function code(){return 'USA-'+Array.from({length:8},()=>ALPHABET[randomInt(ALPHABET.length)]).join('').replace(/(.{4})(.{4})/,'$1-$2');}
function hash(s){return createHash('sha256').update(s).digest('hex');}
function equal(a,b){return typeof a==='string'&&typeof b==='string'&&a.length===b.length&&timingSafeEqual(Buffer.from(a),Buffer.from(b));}
function duration(n){if(!Number.isInteger(n)||n<1||n>3650)throw Error('Duration must be 1–3650 days');return n;}
function usable(l,now=Date.now()) {return !!l&&l.is_active===true&&!['revoked','deleted'].includes(l.status)&&(!l.expires_at||l.expires_at>now);}
function status(l,now=Date.now()){return !l.is_active||l.status==='revoked'?'revoked':l.expires_at&&l.expires_at<=now?'expired':l.activated_at?'active':'unused';}
function activate(l,uid,now=Date.now()){
 if(!usable(l,now))throw Error('License unavailable, expired or revoked');
 if(l.device_binding_id&&l.device_binding_id!==uid)throw Error('License is bound to another device');
 return {...l,device_binding_id:uid,activated_at:l.activated_at||now,expires_at:l.expires_at||now+duration(l.duration_days)*86400000,status:'active',updated_at:now};
}
function publicHost(host){
 if(typeof host!=='string'||host.length>253||!/^[a-zA-Z0-9.-]+$/.test(host)||host.endsWith('.')||host.includes('..')||!host.includes('.'))return false;
 if(isIP(host))return isIP(host)===4&&!/^(0\.|10\.|127\.|169\.254\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|100\.(6[4-9]|[7-9]\d|1[01]\d|12[0-7])\.|192\.0\.|198\.(1[89])\.|22[4-9]\.|2[3-5]\d\.)/.test(host);
 return !/(^|\.)(localhost|local|internal|test|invalid)$/.test(host.toLowerCase())&&/^[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?$/.test(host);
}
function safeUrl(url){try{const u=new URL(url);if(u.protocol==='mailto:')return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(u.pathname)&&!u.search;if(u.protocol!=='https:'||u.username||u.password||u.port)return false;return publicHost(u.hostname);}catch{return false;}}
function ovpnSummary(text){
 if(typeof text!=='string'||Buffer.byteLength(text)>65536)throw Error('Profile must be UTF-8 and under 64 KiB');
 const forbidden=/<\s*(key|pkcs12|secret|tls-auth|tls-crypt|tls-crypt-v2|auth-user-pass)\b|BEGIN .*PRIVATE KEY|^\s*(up|down|plugin|script-security|management|auth-user-pass|key|pkcs12|secret|tls-auth|tls-crypt|tls-crypt-v2|config|include|http-proxy|socks-proxy)\b/im;
 if(forbidden.test(text))throw Error('Profile contains credentials, scripts, management or unsupported secret directives; not imported');
 const clean=text.replace(/<ca>[\s\S]*?<\/ca>/gi,'').replace(/<cert>[\s\S]*?<\/cert>/gi,'');
 if(/[<>]/.test(clean))throw Error('Unsupported inline block');
 const allowed=new Set(['client','dev','proto','remote','remote-cert-tls','verify-x509-name','cipher','data-ciphers','auth','tls-version-min','nobind','resolv-retry','persist-key','persist-tun','verb','redirect-gateway','dhcp-option','auth-nocache','reneg-sec','connect-timeout','float','explicit-exit-notify']);
 const directives=clean.split(/\r?\n/).map(x=>x.trim()).filter(x=>x&&!/^[#;]/.test(x));
 for(const line of directives)if(!allowed.has(line.split(/\s+/)[0]))throw Error('Unsupported directive: '+line.split(/\s+/)[0]);
 const remote=directives.filter(x=>x.startsWith('remote '));if(remote.length!==1)throw Error('Exactly one remote is required');
 const [,host,p]=remote[0].split(/\s+/);const port=Number(p);if(!publicHost(host)||!Number.isInteger(port)||port<1||port>65535)throw Error('Invalid public endpoint');
 const proto=directives.find(x=>x.startsWith('proto '))?.split(/\s+/)[1];if(!['udp','tcp','tcp-client'].includes(proto))throw Error('Unsupported transport');
 const modern=directives.some(x=>/^data-ciphers .*AES-(128|256)-GCM/.test(x))&&!directives.some(x=>/^auth (SHA1|MD5)$/.test(x));
 if(!modern)throw Error('Modern AEAD cipher configuration required');
 return {host,port,transport:proto==='udp'?'udp':'tcp',requires_gateway_verification:true,contains_public_ca:/<ca>/i.test(text)};
}
function server(s){
 for(const k of ['name','country','city','host'])if(typeof s[k]!=='string'||!s[k].trim()||s[k].length>253)throw Error('Invalid '+k);
 if(!publicHost(s.host)||!['udp','tcp'].includes(s.transport)||!Number.isInteger(s.port)||s.port<1||s.port>65535)throw Error('Invalid server endpoint');
 if(!Array.isArray(s.dns)||s.dns.length<1||s.dns.length>3||s.dns.some(x=>!isIP(x)||!publicHost(x)))throw Error('Public IPv4 DNS required');
 if(!Number.isInteger(s.max_sessions)||s.max_sessions<1||s.max_sessions>200)throw Error('Invalid capacity');
 if(!Number.isInteger(s.priority)||s.priority<0||s.priority>10000)throw Error('Invalid priority');
 if(!['primary','backup','standard'].includes(s.role)||s.ipv6_policy!=='block')throw Error('Unsupported role or IPv6 policy');
 if(typeof s.enabled!=='boolean'||typeof s.maintenance!=='boolean'||(s.flag&&typeof s.flag!=='string')||String(s.flag||'').length>8)throw Error('Invalid flags');
 return Object.fromEntries(['name','country','city','flag','host','transport','port','dns','max_sessions','priority','role','enabled','maintenance','maintenance_message','ca_reference','tls_profile_reference','ipv6_policy'].map(k=>[k,s[k]??'']));
}
module.exports={code,hash,equal,duration,usable,status,activate,publicHost,safeUrl,ovpnSummary,server};
