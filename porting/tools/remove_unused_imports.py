from pathlib import Path
import re
removed=0
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();old=s
 body=re.sub(r'^import\s+[^;]+;\s*$', '', s, flags=re.M)
 imports=set()
 def keep(m):
  global removed
  name=m.group(1); simple=name.rsplit('.',1)[-1]
  if name in imports or not re.search(r'\b'+re.escape(simple)+r'\b',body):removed+=1;return ''
  imports.add(name);return m.group(0)
 s=re.sub(r'^import\s+([\w.]+);\n',keep,s,flags=re.M)
 if old!=s:p.write_text(s)
print('Removed unused/duplicate imports:',removed)
