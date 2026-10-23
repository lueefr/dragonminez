from pathlib import Path
import re
lines=[]
for p in sorted(Path('src/main/java').rglob('*.java')):
 if '/platform/' in str(p):continue
 s=p.read_text();package=re.search(r'package ([\w.]+);',s)
 if not package:continue
 pattern=r'@SubscribeEvent(?:\(([^)]*)\))?\s+public\s+static\s+void\s+(\w+)\s*\(\s*((?:com\.dragonminez\.platform\.event\.server\.)?Server(?:AboutToStart|Starting|Started|Stopping|Stopped)Event|(?:LevelEvent|ChunkEvent)\.(?:Load|Unload))\s+\w+\s*\)'
 for m in re.finditer(pattern,s):
  opts,method,event=m.groups();opts=opts or ''
  priority=re.search(r'priority\s*=\s*EventPriority\.(\w+)',opts)
  priority=priority[1] if priority else 'NORMAL'
  canceled='true' if re.search(r'receiveCanceled\s*=\s*true',opts) else 'false'
  lines.append(f'        GameEvents.BUS.addListener({event}.class, EventPriority.{priority}, {canceled}, {package[1]}.{p.stem}::{method});')
name='ServerLifecycleSubscribers'
Path(f'src/main/java/com/dragonminez/platform/event/server/{name}.java').write_text(f'''package com.dragonminez.platform.event.server;

import com.dragonminez.platform.event.*;
import com.dragonminez.platform.event.level.*;

/** Explicit server and world lifecycle subscriptions retaining original priorities. */
public final class {name} {{
    private static boolean installed;
    private {name}() {{}}
    public static synchronized void register() {{
        if (installed) return;
'''+ '\n'.join(lines)+'\n        installed = true;\n    }\n}\n')
print(len(lines),'server/world lifecycle subscribers')
