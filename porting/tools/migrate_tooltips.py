from pathlib import Path
import re
for path in Path('src/main/java').rglob('*.java'):
 s=path.read_text();edits=[]
 for m in re.finditer(r'public void appendHoverText\(([^)]*)\)\s*\{',s):
  params=[re.sub(r'@\w+(?:\.\w+)*\s*','',p.strip()) for p in m[1].split(',')]
  if len(params)!=4:continue
  stack,context,tooltip,flag=[p.split()[-1] for p in params]
  depth=1;i=m.end()
  while depth:
   if s[i]=='{':depth+=1
   if s[i]=='}':depth-=1
   i+=1
  body=s[m.end():i-1]
  if 'context.level()' in body:
   if len(re.findall(r'\blevel\b',body.replace('net.minecraft.world.level.Level','Level')))!=2:
    raise ValueError(f'World-dependent tooltip needs explicit migration: {path}')
   body=re.sub(r'\s*net.minecraft.world.level.Level level = context.level\(\);','',body)
  body=re.sub(r'super\.appendHoverText\(([^,]+),\s*([^,]+),\s*'+tooltip+r',\s*([^)]+)\)',r'super.appendHoverText(\1, \2, dmz$display, '+tooltip+r'::add, \3)',body)
  declaration=f'public void appendHoverText({params[0]}, {params[1]}, net.minecraft.world.item.component.TooltipDisplay dmz$display, java.util.function.Consumer<Component> dmz$consumer, {params[3]}) {{'
  replacement=declaration+f'\n        java.util.List<Component> {tooltip} = new java.util.ArrayList<>();\n        try {{'+body+f'\n        }} finally {{ {tooltip}.forEach(dmz$consumer); }}\n    }}'
  edits.append((m.start(),i,replacement))
 for a,b,value in reversed(edits):s=s[:a]+value+s[b:]
 if edits:path.write_text(s);print(path.name)
