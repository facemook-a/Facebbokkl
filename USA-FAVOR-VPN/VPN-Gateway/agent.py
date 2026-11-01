#!/usr/bin/env python3
"""Root-owned gateway controller. HTTPS is terminated by nginx; no secrets in logs."""
import shutil, concurrent.futures, hashlib, hmac, http.server, ipaddress, json, os, re, socket, socketserver, subprocess, tempfile, threading, time, urllib.request
from pathlib import Path
CONFIG=Path('/etc/usa-favor/gateway.json')
class Gateway:
 def __init__(self, config): self.c=config; self.sessions={}; self.lock=threading.Lock(); self.pki_lock=threading.Lock()
 def authorize(self, sid, token):
  if not re.fullmatch(r'\w{1,80}',sid) or not re.fullmatch(r'[A-Za-z0-9_-]{43}',token): return False
  data=json.dumps({'gateway_id':self.c['gateway_id'],'session_id':sid,'token':token}).encode()
  req=urllib.request.Request(self.c['authorization_url'],data=data,headers={'Content-Type':'application/json','Authorization':'Bearer '+self.c['management_key']})
  try:
   with urllib.request.urlopen(req,timeout=7) as r: result=json.load(r)
   ok=result.get('authorized') is True and result.get('expires_at',0)>time.time()*1000
   if ok:
    with self.lock:self.sessions[sid]=token
   return ok
  except Exception:return False
 def management(self, command):
  with socket.socket(socket.AF_UNIX) as s:
   s.settimeout(3);s.connect('/run/openvpn-server/usa-favor.sock');s.sendall((command+'\nquit\n').encode());out=b''
   while True:
    b=s.recv(65536)
    if not b:break
    out+=b
    if len(out)>1048576:raise ValueError('Management output too large')
  return out.decode(errors='replace')
 def clients(self):
  result=[]
  for line in self.management('status 3').splitlines():
   cols=line.split('\t')
   if cols[0]=='CLIENT_LIST' and len(cols)>7:
    result.append({'session_id':cols[1],'bytes_received':int(cols[5]),'bytes_sent':int(cols[6])})
  return result
 def enforce(self):
  failures=0
  auth_pool=concurrent.futures.ThreadPoolExecutor(max_workers=32)
  revoke_pool=concurrent.futures.ThreadPoolExecutor(max_workers=1)
  time.sleep(5)
  while True:
   try:
    clients=self.clients()
    with self.lock:tokens=dict(self.sessions)
    def permitted(client):
     sid=client['session_id']
     return bool(re.fullmatch(r'\w{1,80}',sid) and sid in tokens and self.authorize(sid,tokens[sid]))
    results=list(auth_pool.map(permitted,clients))
    for client,ok in zip(clients,results):
     sid=client['session_id']
     if not ok and re.fullmatch(r'\w{1,80}',sid):
      self.management('kill '+sid)
      revoke_pool.submit(self.revoke,sid)
      with self.lock:self.sessions.pop(sid,None)
    failures=0
   except Exception:
    failures+=1
    if failures>=3:
     # A running tunnel without a functioning enforcement channel must not persist.
     subprocess.run(['/usr/bin/systemctl','stop','openvpn-server@usa-favor'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=10)
   time.sleep(15)
 def revoke(self,sid):
  if not re.fullmatch(r'\w{1,80}',sid):return
  base='/var/lib/usa-favor/pki-work'
  with self.pki_lock:
   env={**os.environ,'EASYRSA_BATCH':'1'}
   result=subprocess.run([base+'/easyrsa','revoke',sid],cwd=base,env=env,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=20)
   if result.returncode==0:
    subprocess.run([base+'/easyrsa','gen-crl'],cwd=base,env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=20)
    shutil.copyfile(base+'/pki/crl.pem','/etc/openvpn/server/crl.pem')
 def renew_crl(self):
  while True:
   try:
    base='/var/lib/usa-favor/pki-work'
    with self.pki_lock:
     subprocess.run([base+'/easyrsa','gen-crl'],cwd=base,env={**os.environ,'EASYRSA_BATCH':'1','EASYRSA_CRL_DAYS':'30'},check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=20)
     shutil.copyfile(base+'/pki/crl.pem','/etc/openvpn/server/crl.pem')
   except Exception:print('CRL refresh failed; investigate issuer availability',flush=True)
   time.sleep(43200)
 def certificate(self,sid):
  if not re.fullmatch(r'\w{1,80}',sid):raise ValueError('Invalid identity')
  base=Path('/var/lib/usa-favor/pki-work')
  with self.pki_lock:
   env={**os.environ,'EASYRSA_BATCH':'1','EASYRSA_CERT_EXPIRE':'1','EASYRSA_ALGO':'ec','EASYRSA_CURVE':'prime256v1'}
   subprocess.run([str(base/'easyrsa'),'build-client-full',sid,'nopass'],cwd=base,env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=30)
   key=base/'pki/private'/f'{sid}.key';cert=(base/'pki/issued'/f'{sid}.crt').read_text()
   private=key.read_text();key.unlink()
   cert=cert[cert.index('-----BEGIN CERTIFICATE-----'):]
   return cert,private
 def profile(self,sid,token):
  if not self.authorize(sid,token):raise ValueError('Session not authorized')
  if len(self.clients())>=self.c['max_sessions']:raise ValueError('Gateway capacity reached')
  with tempfile.TemporaryDirectory(dir='/run/usa-favor') as d:
   out=Path(d)/'tls.key'
   subprocess.run(['/usr/sbin/openvpn','--tls-crypt-v2','/etc/openvpn/server/tls-crypt-v2.key','--genkey','tls-crypt-v2-client',str(out)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=10)
   tls=out.read_text()
  ca=Path('/etc/openvpn/server/ca.crt').read_text()
  cert,private=self.certificate(sid)
  return '\n'.join(['client','dev tun','proto '+('udp' if self.c['transport']=='udp' else 'tcp-client'),'remote '+self.c['host']+' '+str(self.c['port']),'nobind','persist-key','persist-tun','remote-cert-tls server','verify-x509-name '+self.c['host']+' name','tls-version-min 1.2','data-ciphers AES-256-GCM:AES-128-GCM','auth SHA256','auth-nocache','reneg-sec 0','redirect-gateway def1 ipv6','block-ipv6','verb 2','<ca>',ca,'</ca>','<cert>',cert,'</cert>','<key>',private,'</key>','<tls-crypt-v2>',tls,'</tls-crypt-v2>','<auth-user-pass>',sid,token,'</auth-user-pass>',''])
class Control(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_POST(self):
  try:
   if not hmac.compare_digest(self.headers.get('Authorization',''),'Bearer '+self.server.g.c['management_key']):return self.reply(403,{})
   size=int(self.headers.get('Content-Length','0'))
   if size<0 or size>4096:return self.reply(413,{})
   data=json.loads(self.rfile.read(size) or b'{}');g=self.server.g
   if self.path=='/control/health':
    clients=g.clients();v={k:g.c[k] for k in ['gateway_id','host','port','transport','dns','egress_ip']};v.update(online=True,protocol='openvpn',active_sessions=len(clients),config_version=g.c['config_version']);return self.reply(200,v)
   if self.path=='/control/profile':return self.reply(200,{'profile':g.profile(data['session_id'],data['token']),'egress_ip':g.c['egress_ip']})
   if self.path=='/control/configure':
    port=data['port'];transport=data['transport'];dns=data['dns'];maximum=data['max_sessions']
    if not isinstance(port,int) or not 1<=port<=65535 or transport not in ['udp','tcp'] or not isinstance(maximum,int) or not 1<=maximum<=200:raise ValueError('Invalid transport')
    if not isinstance(dns,list) or not 1<=len(dns)<=3 or any(not ipaddress.IPv4Address(x).is_global for x in dns):raise ValueError('Invalid DNS')
    conf=Path('/etc/openvpn/server/usa-favor.conf');before=conf.read_text()
    after=re.sub(r'^port .*$',f'port {port}',before,flags=re.M)
    after=re.sub(r'^proto .*$',f"proto {'udp' if transport=='udp' else 'tcp-server'}",after,flags=re.M)
    after=re.sub(r'^max-clients .*$',f'max-clients {maximum}',after,flags=re.M)
    after=re.sub(r'^push "dhcp-option DNS .*"\n','',after,flags=re.M)
    after+=''.join(f'push "dhcp-option DNS {ip}"\n' for ip in dns)
    conf.write_text(after)
    try:subprocess.run(['/usr/bin/systemctl','restart','openvpn-server@usa-favor'],check=True,timeout=20)
    except Exception:
     conf.write_text(before);subprocess.run(['/usr/bin/systemctl','restart','openvpn-server@usa-favor'],timeout=20);raise
    g.c.update(port=port,transport=transport,dns=dns,max_sessions=maximum,config_version=g.c['config_version']+1)
    CONFIG.write_text(json.dumps(g.c,indent=2))
    return self.reply(200,{'ok':True,'message':'Gateway reconfigured. Test and publish the registration.'})
   if self.path=='/control/rotate':
    # TLS crypt v2 server key rotation invalidates old wrapping keys. All live sessions terminate.
    for c in g.clients():g.management('kill '+c['session_id'])
    subprocess.run(['/usr/sbin/openvpn','--genkey','tls-crypt-v2-server','/etc/openvpn/server/tls-crypt-v2.key'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    subprocess.run(['/usr/bin/systemctl','restart','openvpn-server@usa-favor'],check=True)
    return self.reply(200,{'ok':True,'message':'TLS wrapping key rotated; clients must reconnect'})
   self.reply(404,{})
  except Exception:self.reply(503,{'error':'Gateway operation unavailable'})
 def reply(self,status,data):
  body=json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
class AuthHandler(socketserver.StreamRequestHandler):
 def handle(self):
  try:
   raw=self.rfile.readline(4097)
   if len(raw)>4096:raise ValueError()
   d=json.loads(raw);ok=self.server.g.authorize(d['username'],d['password']);self.wfile.write(b'OK\n' if ok else b'DENIED\n')
  except Exception:self.wfile.write(b'DENIED\n')
def validate(c):
 if not re.fullmatch(r'[\w-]{1,80}',c['gateway_id']):raise ValueError('Invalid gateway id')
 if not re.fullmatch(r'[a-zA-Z0-9.-]+',c['host']) or '.' not in c['host']:raise ValueError('Invalid hostname')
 if not re.fullmatch(r'[A-Za-z0-9_-]{43,128}',c['management_key']):raise ValueError('Use at least 32 random bytes')
 if c['authorization_url']!='https://us-central1-usa-favor.cloudfunctions.net/gatewayAuthorize':raise ValueError('Unexpected Firebase endpoint')
 if not ipaddress.ip_address(c['egress_ip']).is_global:raise ValueError('A real public egress IP is required')
 if c['transport'] not in ['tcp','udp'] or type(c['port']) is not int or not 1<=c['port']<=65535:raise ValueError('Invalid transport')
 if type(c['max_sessions']) is not int or not 1<=c['max_sessions']<=200:raise ValueError('Capacity must be 1–200')
 if not isinstance(c['dns'],list) or not 1<=len(c['dns'])<=3:raise ValueError('One to three DNS addresses required')
 for x in c['dns']:
  if not ipaddress.IPv4Address(x).is_global:raise ValueError('Public DNS required')
 return c
if __name__=='__main__':
 os.umask(0o077);g=Gateway(validate(json.loads(CONFIG.read_text())))
 Path('/run/usa-favor').mkdir(exist_ok=True)
 p='/run/usa-favor/auth.sock'
 if os.path.exists(p):os.unlink(p)
 auth=socketserver.ThreadingUnixStreamServer(p,AuthHandler);auth.g=g;os.chmod(p,0o666)
 threading.Thread(target=auth.serve_forever,daemon=True).start();threading.Thread(target=g.enforce,daemon=True).start();threading.Thread(target=g.renew_crl,daemon=True).start()
 server=http.server.ThreadingHTTPServer(('127.0.0.1',9080),Control);server.g=g;server.serve_forever()
