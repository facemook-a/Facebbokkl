import unittest
from unittest.mock import patch
from agent import Gateway,validate
class Tests(unittest.TestCase):
 def test_bad_sessions_fail_closed(self):
  g=Gateway({})
  self.assertFalse(g.authorize('../x','x'*43))
  self.assertFalse(g.authorize('valid','short'))
 def test_management_metrics(self):
  g=Gateway({})
  with patch.object(g,'management',return_value='CLIENT_LIST\tabc\t1.2.3.4\t10.87.0.2\t::\t123\t456\tx\nEND'):
   self.assertEqual(g.clients(),[{'session_id':'abc','bytes_received':123,'bytes_sent':456}])
 def test_unreachable_backend_denies(self):
  g=Gateway({'gateway_id':'one','authorization_url':'https://example.invalid','management_key':'x'*43})
  with patch('urllib.request.urlopen',side_effect=TimeoutError):self.assertFalse(g.authorize('abc','x'*43))
if __name__=='__main__':unittest.main()
