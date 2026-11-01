"""Trusted VPS authorization for Firestore Spark. No Cloud Functions/Secret Manager required.
Service-account ADC remains root-only on the owned gateway, never in Android.
"""
import datetime, hashlib, hmac, secrets, threading, time
UTC=datetime.timezone.utc
class FirebaseStore:
 def __init__(self,config):
  import firebase_admin
  from firebase_admin import auth,firestore,app_check
  if config.get('firebase_project')!='usa-favor':raise ValueError('Unexpected Firebase project')
  try:self.app=firebase_admin.get_app()
  except ValueError:self.app=firebase_admin.initialize_app(options={'projectId':'usa-favor'})
  self.auth=auth;self.fs=firestore;self.app_check=app_check;self.db=firestore.client(app=self.app);self.c=config
  self.lock=threading.Lock();self.cache={};self.policy_cache=None
 def identity(self,authorization,appcheck=None,admin=False):
  if not authorization.startswith('Firebase '):raise ValueError('Firebase ID token required')
  claims=self.auth.verify_id_token(authorization[9:],app=self.app,check_revoked=True)
  if admin and claims.get('admin') is not True:raise ValueError('Administrator required')
  if self.c.get('enforce_app_check') is True:
   if not appcheck:raise ValueError('App Check required')
   self.app_check.verify_token(appcheck,app=self.app)
  return claims['uid']
 @staticmethod
 def epoch(value):
  if isinstance(value,datetime.datetime):return value.timestamp()
  return None
 @classmethod
 def expiry(cls,l):
  explicit=cls.epoch(l.get('expires_at'))
  if explicit is not None:return explicit
  anchor=cls.epoch(l.get('created_at') if l.get('activation_policy')=='generation' else l.get('activated_at'))
  return anchor+int(l.get('duration_days',0))*86400 if anchor is not None else 0
 @classmethod
 def valid(cls,l,uid):
  return bool(l and l.get('schema_version')==3 and l.get('is_active') is True and l.get('status')!='revoked' and l.get('device_binding_id')==uid and cls.expiry(l)>time.time())
 def policy(self):
  with self.lock:cached=self.policy_cache
  if cached and cached[0]>time.monotonic():return cached[1]
  server=self.db.document('servers/'+self.c['gateway_id']).get(timeout=7,retry=None).to_dict() or {}
  settings=self.db.document('configuration/client').get(timeout=7,retry=None).to_dict() or {}
  allowed=server.get('enabled') is True and server.get('published') is True and server.get('verified') is True and not server.get('maintenance') and not settings.get('maintenance')
  result=(allowed,int(settings.get('minimum_version',1)))
  with self.lock:self.policy_cache=(time.monotonic()+45,result)
  return result
 def authorize(self,sid,token):
  now=time.time();digest=hashlib.sha256(token.encode()).hexdigest()
  with self.lock:cached=self.cache.get(sid)
  if cached and cached['until']>time.monotonic():return hmac.compare_digest(digest,cached['hash']) and cached['expires']>now
  try:
   s=self.db.document('sessions/'+sid).get(timeout=7,retry=None).to_dict() or {}
   if not s or s.get('server_id')!=self.c['gateway_id'] or s.get('revoked') is not False or not hmac.compare_digest(digest,s.get('token_hash','')):return False
   l=self.db.document('licenses/'+s['license_id']).get(timeout=7,retry=None).to_dict() or {}
   policy,minimum=self.policy()
   expires=min(self.epoch(s.get('expires_at')) or 0,self.expiry(l))
   allowed=policy and int(s.get('version',0))>=minimum and expires>now and self.valid(l,s.get('uid'))
   if allowed:
    with self.lock:self.cache[sid]={'until':min(time.monotonic()+45,self.policy_cache[0] if self.policy_cache else time.monotonic()),'expires':expires,'hash':digest}
   return allowed
  except Exception:return False
 def new_session(self,uid,version):
  allowed,minimum=self.policy()
  if not allowed or not isinstance(version,int) or version<minimum:raise ValueError('Gateway maintenance or app update required')
  b=self.db.document('bindings/'+uid).get(timeout=7,retry=None).to_dict() or {};lid=b.get('license_id')
  if not lid:raise ValueError('Activate a license first')
  license_ref=self.db.document('licenses/'+lid);lock_ref=self.db.document('session_locks/'+lid)
  token=secrets.token_urlsafe(32);ref=self.db.collection('sessions').document();now=time.time()
  @self.fs.transactional
  def create(t):
   l=license_ref.get(transaction=t).to_dict() or {};lock=lock_ref.get(transaction=t).to_dict() or {}
   if not self.valid(l,uid):raise ValueError('License revoked, expired or bound elsewhere')
   old_ref=self.db.document('sessions/'+lock['session_id']) if lock.get('session_id') else None
   old=old_ref.get(transaction=t).to_dict() if old_ref else None
   expires=min(self.expiry(l),now+3600)
   if old_ref and old:t.update(old_ref,{'revoked':True})
   t.set(ref,{'uid':uid,'license_id':lid,'server_id':self.c['gateway_id'],'token_hash':hashlib.sha256(token.encode()).hexdigest(),'created_at':self.fs.SERVER_TIMESTAMP,'expires_at':datetime.datetime.fromtimestamp(expires,UTC),'revoked':False,'version':version})
   t.set(lock_ref,{'session_id':ref.id,'until':datetime.datetime.fromtimestamp(expires,UTC)})
   return expires
  expires=create(self.db.transaction())
  return ref.id,token,int(expires*1000)
 def end(self,uid,sid):
  ref=self.db.document('sessions/'+sid);s=ref.get(timeout=7,retry=None).to_dict() or {}
  if s.get('uid')!=uid:raise ValueError('Unknown session')
  ref.update({'revoked':True})
  with self.lock:self.cache.pop(sid,None)
