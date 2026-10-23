from pathlib import Path
from zipfile import ZipFile
from collections import defaultdict
import re
jar=Path('/home/lucas/Downloads/.gradle-dragonminez-fabric/caches/fabric-loom/26.1.2/minecraft-merged.jar')
classes=set();by_name=defaultdict(list)
with ZipFile(jar) as z:
 for entry in z.namelist():
  if entry.endswith('.class') and '$' not in entry:
   name=entry[:-6].replace('/','.');classes.add(name);by_name[name.rsplit('.',1)[-1]].append(name)
count=0
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();orig=s
 def migrate(m):
  global count
  name=m[1];candidates=by_name[name.rsplit('.',1)[-1]]
  if name not in classes and len(candidates)==1:
   count+=1;return 'import '+candidates[0]+';'
  return m[0]
 s=re.sub(r'import (net\.minecraft\.[\w.]+);',migrate,s)
 if s!=orig:p.write_text(s)
print(f'Migrated {count} unambiguous native class imports')
