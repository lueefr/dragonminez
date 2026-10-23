from pathlib import Path
import re
exec(Path('porting/tools/migrate_entity_serialization.py').read_text().split('count=0')[0])
count=0
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();orig=s
 edits=[]
 for m in re.finditer(r'public boolean hurt\(DamageSource (\w+), float (\w+)\)\s*\{',s):
  source,amount=m.groups();a=m.end()-1;b=body_end(s,a);body=s[a+1:b]
  body=body.replace('super.hurt(', 'super.hurtServer(dmz$level, ')
  replacement=f'public boolean hurtServer(net.minecraft.server.level.ServerLevel dmz$level, DamageSource {source}, float {amount}) {{'+body+'}'
  edits.append((m.start(),b+1,replacement));count+=1
 for a,b,replacement in reversed(edits):s=s[:a]+replacement+s[b:]
 s=re.sub(r'\.hurt\(','.hurtOrSimulate(',s)
 # Native 26.1 uses a double for the accumulated fall distance.
 s=re.sub(r'causeFallDamage\(float (\w+),',r'causeFallDamage(double \1,',s)
 if s!=orig:p.write_text(s)
print('Migrated',count,'server damage overrides and their callers')
