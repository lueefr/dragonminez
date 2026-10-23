from pathlib import Path
import re

def body_end(s,start):
 depth=1;i=start+1;quote=None;escape=False;line=False;block=False
 while i<len(s):
  c=s[i];n=s[i:i+2]
  if line:
   if c=='\n':line=False
  elif block:
   if n=='*/':block=False;i+=1
  elif quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
  elif n=='//':line=True;i+=1
  elif n=='/*':block=True;i+=1
  elif c in ('"',"'"):quote=c
  elif c=='{':depth+=1
  elif c=='}':
   depth-=1
   if depth==0:return i
  i+=1
 raise ValueError('Unclosed body')
count=0
for p in Path('src/main/java/com/dragonminez/common/init/entities').rglob('*.java'):
 s=p.read_text();orig=s
 pattern=r'\b(public|protected) void (addAdditionalSaveData|readAdditionalSaveData)\(CompoundTag (\w+)\)\s*\{'
 edits=[]
 for m in re.finditer(pattern,s):
  access,method,var=m.groups();a=m.end()-1;b=body_end(s,a);body=s[a+1:b]
  output=method=='addAdditionalSaveData';native='ValueOutput' if output else 'ValueInput';param='dmz$output' if output else 'dmz$input'
  body=re.sub(r'super\.'+method+r'\(\s*'+var+r'\s*\)',f'super.{method}({param})',body)
  signature=f'{access} void {method}(net.minecraft.world.level.storage.{native} {param}) {{'
  if output:
   replacement=signature+f'\n        CompoundTag {var} = new CompoundTag();\n        try {{'+body+f'\n        }} finally {{\n            com.dragonminez.platform.nbt.NbtValues.write({param}, {var});\n        }}\n    }}'
  else:
   replacement=signature+f'\n        CompoundTag {var} = com.dragonminez.platform.nbt.NbtValues.read({param});'+body+'}'
  edits.append((m.start(),b+1,replacement));count+=1
 for a,b,replacement in reversed(edits):s=s[:a]+replacement+s[b:]
 if s!=orig:p.write_text(s)
print('Migrated',count,'entity serialization overrides, preserving flat NBT keys')
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();new=s.replace('MobSpawnType', 'EntitySpawnReason').replace('net.minecraft.world.item.UseAnim;', 'net.minecraft.world.item.ItemUseAnimation;')
 if 'import net.minecraft.world.item.ItemUseAnimation;' in new:new=re.sub(r'\bUseAnim\b','ItemUseAnimation',new)
 if new!=s:p.write_text(new)
