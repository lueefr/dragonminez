from pathlib import Path
import re
out={'common':[], 'client':[]}
for p in sorted(Path('src/main/java').rglob('*.java')):
 s=p.read_text()
 if '/platform/' in str(p):continue
 package=re.search(r'package ([\w.]+);',s)
 if not package:continue
 pattern=r'@SubscribeEvent(?:\(([^)]*)\))?\s+public\s+static\s+void\s+(\w+)\s*\(\s*(?:com\.dragonminez\.platform\.event\.tick\.)?((?:Client|Server|Player|Entity|Level)TickEvent\.(?:Pre|Post))\s+\w+\s*\)'
 for m in re.finditer(pattern,s):
  opts,method,event=m.groups();opts=opts or ''
  priority=re.search(r'priority\s*=\s*EventPriority\.(\w+)',opts)
  priority=priority[1] if priority else 'NORMAL'
  canceled='true' if re.search(r'receiveCanceled\s*=\s*true',opts) else 'false'
  side='client' if '/client/' in str(p) or 'EnvType.CLIENT)' in s[:s.index('public ')] else 'common'
  out[side].append(f'        GameEvents.BUS.addListener({event}.class, EventPriority.{priority}, {canceled}, {package[1]}.{p.stem}::{method});')
for side,lines in out.items():
 name=side.title()+'TickSubscribers'
 code=f'''package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.*;

/** Explicit subscriptions generated from the original annotated gameplay handlers. */
public final class {name} {{
    private static boolean installed;
    private {name}() {{}}
    public static synchronized void register() {{
        if (installed) return;
'''+ '\n'.join(lines)+'\n        installed = true;\n    }\n}\n'
 Path(f'src/main/java/com/dragonminez/platform/event/tick/{name}.java').write_text(code)
 print(side,len(lines))
