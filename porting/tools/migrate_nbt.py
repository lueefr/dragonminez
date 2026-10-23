"""Migrate typed NBT variables to Minecraft 26.1 APIs without changing stored keys."""
from pathlib import Path
import re

def args_at(s,start):
 depth=1;quote=None;escape=False;parts=[];last=start;i=start
 while i<len(s):
  c=s[i]
  if quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c=='(':depth+=1
  elif c==')':
   depth-=1
   if depth==0:return parts+[s[last:i]],i+1
  elif c==',' and depth==1:parts.append(s[last:i]);last=i+1
  i+=1
 return None,None

counts={}
for p in Path('src/main/java').rglob('*.java'):
 if 'platform/nbt' in str(p):continue
 s=p.read_text();old=s
 compounds=set(re.findall(r'\bCompoundTag\s+(\w+)',s));lists=set(re.findall(r'\bListTag\s+(\w+)',s))
 if not compounds and not lists:continue
 pattern=re.compile(r'\b((?:this\.)?\w+)\.(getInt|getLong|getFloat|getDouble|getByte|getShort|getString|getBoolean|getCompound|getList|getIntArray|getLongArray|getByteArray|getAllKeys|contains|hasUUID|getUUID|putUUID)\(')
 replacements=[]
 for m in pattern.finditer(s):
  receiver,method=m.group(1),m.group(2);var=receiver.removeprefix('this.')
  if var not in compounds|lists:continue
  args,end=args_at(s,m.end())
  if end is None:continue
  if method in ("getIntArray","getLongArray","getByteArray") and s[end:].startswith(".orElse"):continue
  args=[a.strip() for a in args];value=None
  defaults={'getInt':'0','getLong':'0L','getFloat':'0F','getDouble':'0D','getByte':'(byte) 0','getShort':'(short) 0','getString':'""','getBoolean':'false'}
  if method in defaults and len(args)==1:value=f'{receiver}.{method}Or({args[0]}, {defaults[method]})'
  elif method=='getCompound' and len(args)==1:value=f'{receiver}.getCompoundOrEmpty({args[0]})'
  elif method=='getList' and len(args)==2 and var in compounds:value=f'com.dragonminez.platform.nbt.NbtCompat.getList({receiver}, {args[0]}, {args[1]})'
  elif method=='contains' and len(args)==2 and var in compounds:value=f'com.dragonminez.platform.nbt.NbtCompat.contains({receiver}, {args[0]}, {args[1]})'
  elif method=='getAllKeys':value=f'{receiver}.keySet()'
  elif method in ('hasUUID','getUUID','putUUID') and var in compounds:value=f'com.dragonminez.platform.nbt.NbtCompat.{method}({receiver}, '+', '.join(args)+')'
  elif method in ('getIntArray','getLongArray','getByteArray') and len(args)==1:
   primitive={'getIntArray':'int','getLongArray':'long','getByteArray':'byte'}[method]
   value=f'{receiver}.{method}({args[0]}).orElseGet(() -> new {primitive}[0])'
  if value is not None:replacements.append((m.start(),end,value));counts[method]=counts.get(method,0)+1
 # Avoid overlapping edits: outer conversions can contain a second NBT call. Re-run for remaining nested calls.
 last=len(s)
 for a,b,value in reversed(replacements):
  if b<=last:s=s[:a]+value+s[b:];last=a
 if s!=old:p.write_text(s)
print(counts)
