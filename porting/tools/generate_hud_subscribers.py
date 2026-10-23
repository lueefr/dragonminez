from pathlib import Path
import re
lines=[]
for p in sorted(Path('src/main/java/com/dragonminez/client').rglob('*.java')):
 s=p.read_text();package=re.search(r'package ([\w.]+);',s)
 if not package:continue
 pattern=r'@SubscribeEvent(?:\(([^)]*)\))?\s+public\s+static\s+void\s+(\w+)\s*\(\s*RenderGuiLayerEvent\.(Pre|Post)\s+\w+\s*\)'
 for m in re.finditer(pattern,s):
  opts,method,event=m.groups();opts=opts or ''
  priority=re.search(r'priority\s*=\s*EventPriority\.(\w+)',opts)
  priority=priority[1] if priority else 'NORMAL'
  canceled='true' if re.search(r'receiveCanceled\s*=\s*true',opts) else 'false'
  lines.append(f'        GameEvents.BUS.addListener(RenderGuiLayerEvent.{event}.class, EventPriority.{priority}, {canceled}, {package[1]}.{p.stem}::{method});')
name='HudSubscribers'
Path(f'src/main/java/com/dragonminez/platform/event/client/{name}.java').write_text(f'''package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.*;
public final class {name} {{
    private static boolean installed;
    private {name}() {{}}
    public static synchronized void register() {{
        if (installed) return;
'''+ '\n'.join(lines)+'\n        installed = true;\n    }\n}\n')
print(len(lines),'HUD subscribers')
