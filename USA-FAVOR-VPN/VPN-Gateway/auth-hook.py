#!/usr/bin/python3
import json,socket,sys,os
try:
 with open(sys.argv[1]) as f:
  username=f.readline().strip();password=f.readline().strip()
 if os.environ.get('common_name')!=username:sys.exit(1)
 with socket.socket(socket.AF_UNIX) as s:
  s.settimeout(10);s.connect('/run/usa-favor/auth.sock');s.sendall((json.dumps({'username':username,'password':password})+'\n').encode());s.shutdown(socket.SHUT_WR);ok=s.recv(64)==b'OK\n'
 sys.exit(0 if ok else 1)
except Exception:sys.exit(1)
