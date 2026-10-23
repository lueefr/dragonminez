from pathlib import Path
import re
exec(Path('porting/tools/migrate_nbt.py').read_text().split('counts={}')[0])
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();orig=s
 s=s.replace('event.getEntity().getPersistentData()', 'com.dragonminez.platform.entity.EntityData.get(event.getEntity())').replace('proj.getOwner().getPersistentData()', 'com.dragonminez.platform.entity.EntityData.get(proj.getOwner())')
 pattern=re.compile(r'com\.dragonminez\.platform\.entity\.EntityData\.get\(')
 edits=[]
 for m in pattern.finditer(s):
  _,receiver_end=args_at(s,m.end())
  if receiver_end is None:continue
  method_match=re.match(r'\.(getInt|getLong|getFloat|getDouble|getByte|getShort|getString|getBoolean|getCompound|getList|contains|hasUUID|getUUID|putUUID)\(',s[receiver_end:])
  if not method_match:continue
  method=method_match[1];receiver=s[m.start():receiver_end]
  args,end=args_at(s,receiver_end+method_match.end())
  if end is None:continue
  args=[a.strip() for a in args];value=None
  defaults={'getInt':'0','getLong':'0L','getFloat':'0F','getDouble':'0D','getByte':'(byte) 0','getShort':'(short) 0','getString':'""','getBoolean':'false'}
  if method in defaults and len(args)==1:value=f'{receiver}.{method}Or({args[0]}, {defaults[method]})'
  elif method=='getCompound' and len(args)==1:value=f'{receiver}.getCompoundOrEmpty({args[0]})'
  elif method=='getList' and len(args)==2:value=f'com.dragonminez.platform.nbt.NbtCompat.getList({receiver}, {args[0]}, {args[1]})'
  elif method=='contains' and len(args)==2:value=f'com.dragonminez.platform.nbt.NbtCompat.contains({receiver}, {args[0]}, {args[1]})'
  elif method in ('hasUUID','getUUID','putUUID'):value=f'com.dragonminez.platform.nbt.NbtCompat.{method}({receiver}, '+', '.join(args)+')'
  if value is not None:edits.append((m.start(),end,value))
 last=len(s)
 for a,b,value in reversed(edits):
  if b<=last:s=s[:a]+value+s[b:];last=a
 if s!=orig:p.write_text(s)
