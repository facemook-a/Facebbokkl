import datetime,hashlib,threading,time,unittest
from unittest.mock import Mock
from firebase_store import FirebaseStore
UTC=datetime.timezone.utc
def license(**changes):
 now=datetime.datetime.now(UTC)
 value={'schema_version':3,'is_active':True,'status':'active','device_binding_id':'owner','activation_policy':'first_activation','duration_days':1,'activated_at':now-datetime.timedelta(hours=1),'created_at':now-datetime.timedelta(days=2),'expires_at':None}
 value.update(changes);return value
class FirebaseAuthorizationTests(unittest.TestCase):
 def test_license_is_bound_to_one_identity(self):
  self.assertTrue(FirebaseStore.valid(license(),'owner'))
  self.assertFalse(FirebaseStore.valid(license(),'another-device'))
 def test_revocation_and_expiration_are_independent_of_ui(self):
  self.assertFalse(FirebaseStore.valid(license(is_active=False,status='revoked'),'owner'))
  self.assertFalse(FirebaseStore.valid(license(expires_at=datetime.datetime.now(UTC)-datetime.timedelta(seconds=1)),'owner'))
 def test_generation_and_first_activation_use_different_anchors(self):
  self.assertTrue(FirebaseStore.valid(license(),'owner'))
  self.assertFalse(FirebaseStore.valid(license(activation_policy='generation'),'owner'))
 def test_missing_activation_or_unknown_schema_cannot_connect(self):
  self.assertFalse(FirebaseStore.valid(license(activated_at=None),'owner'))
  self.assertFalse(FirebaseStore.valid(license(schema_version=1),'owner'))
 def test_cached_session_cannot_outlive_expiry_or_accept_different_token(self):
  store=object.__new__(FirebaseStore);store.lock=threading.Lock();store.cache={'session':{'until':time.monotonic()+30,'expires':time.time()+30,'hash':hashlib.sha256(b'right').hexdigest()}}
  self.assertFalse(store.authorize('session','wrong'));self.assertTrue(store.authorize('session','right'))
  store.cache['session']['expires']=time.time()-1
  self.assertFalse(store.authorize('session','right'))
 def test_database_failure_denies_instead_of_faking_authorization(self):
  store=object.__new__(FirebaseStore);store.lock=threading.Lock();store.cache={};store.db=Mock();store.db.document.side_effect=TimeoutError
  self.assertFalse(store.authorize('session','token'))
if __name__=='__main__':unittest.main()
