#!/usr/bin/env python3
import json,sys,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parent.parent
for app,package in [('Admin-App','com.b5studios.usafavor'),('Client-App','com.usafavor.client')]:
 folder=root/app/'app';path=folder/'google-services.json'
 if not path.exists():sys.exit(f'Missing {app}/app/google-services.json; download it for {package} from Firebase usa-favor')
 d=json.loads(path.read_text());assert d['project_info']['project_id']=='usa-favor','Wrong Firebase project'
 clients=[c for c in d['client'] if c['client_info']['android_client_info']['package_name']==package]
 assert len(clients)==1,'Missing or duplicate matching registration'
 c=clients[0];assert c['client_info']['mobilesdk_app_id'].startswith('1:'+d['project_info']['project_number']+':android:'),'Invalid project/app ID pairing'
 assert c.get('api_key') and c['api_key'][0].get('current_key'),'Missing Firebase Android API key'
 generated=folder/'build/generated/res/google-services/debug/values/values.xml'
 if generated.exists():
  values={x.attrib['name']:x.text for x in ET.parse(generated).getroot()}
  assert values['google_app_id']==c['client_info']['mobilesdk_app_id'],'Build used wrong Android registration'
  assert values['project_id']=='usa-favor'
  assert values['google_api_key']==c['api_key'][0]['current_key'],'Build used wrong API key'
 print(f'{app}: {package} — project and selected registration verified; no credential values printed')
