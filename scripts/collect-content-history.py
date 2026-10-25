#!/usr/bin/env python3
"""Collect real historical JSON layouts from reachable git objects without checking out old code."""
import json
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PREFIX='src/main/resources/data/dragonminez/'
def kind(path):
 if '/previousQuests/' in path:return 'SAGA' if '/sagas/' in path else 'QUEST'
 if '/previousConfigs/forms/' in path or '/forms/' in path:return 'FORMS'
 for suffix,value in [('skills.json','SKILLS'),('character.json','RACE_CHARACTER'),('stats.json','RACE_STATS'),('entities.json','ENTITIES'),('techniques.json','TECHNIQUES')]:
  if path.endswith('/'+suffix):return value
 return None
def shape(value):
 if isinstance(value,dict):return {key:shape(v) for key,v in sorted(value.items())}
 if isinstance(value,list):return sorted({json.dumps(shape(v),sort_keys=True) for v in value})
 return type(value).__name__
objects=subprocess.check_output(['git','rev-list','--objects','--all','--',PREFIX+'previousConfigs',PREFIX+'previousQuests'],cwd=ROOT,text=True)
entries=[]
for line in objects.splitlines():
 bits=line.split(' ',1)
 if len(bits)==2 and bits[1].endswith('.json') and kind(bits[1]):entries.append((bits[0],bits[1]))
# cat-file batch avoids one git process per fixture.
payload=subprocess.run(['git','cat-file','--batch'],cwd=ROOT,input=('\n'.join(oid for oid,_ in entries)+'\n').encode(),stdout=subprocess.PIPE,check=True).stdout
cursor=0;layouts={}
for oid,path in entries:
 end=payload.index(b'\n',cursor);header=payload[cursor:end].split();size=int(header[2]);cursor=end+1
 data=payload[cursor:cursor+size];cursor+=size+1
 if header[1]!=b'blob':continue
 try:document=json.loads(data)
 except (ValueError,UnicodeError):continue
 if not isinstance(document,dict):continue
 category=kind(path);fingerprint=(category,json.dumps(shape(document),sort_keys=True))
 if fingerprint not in layouts:layouts[fingerprint]={'kind':category,'source':oid+':'+path,'document':document}
fixtures=sorted(layouts.values(),key=lambda x:(x['kind'],x['source']))
target=ROOT/'src/test/resources/content-history/content.json'
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(fixtures,ensure_ascii=False,indent=2)+'\n')
print(len(entries),'historical documents;',len(fixtures),'distinct layouts:',', '.join(sorted({f['kind'] for f in fixtures})))
