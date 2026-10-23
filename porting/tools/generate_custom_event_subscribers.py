from pathlib import Path
import re
out={'Common':[], 'Client':[]}
for p in sorted(Path('src/main/java').rglob('*.java')):
 if '/platform/' in str(p):continue
 s=p.read_text();package=re.search(r'package ([\w.]+);',s)
 if not package:continue
 pattern=r'@SubscribeEvent(?:\(([^)]*)\))?\s+public\s+static\s+void\s+(\w+)\s*\(\s*(?:com\.dragonminez\.common\.events\.)?DMZEvent\.([\w.]+)\s+\w+\s*\)'
 for m in re.finditer(pattern,s):
  opts,method,event=m.groups();opts=opts or ''
  priority=re.search(r'priority\s*=\s*EventPriority\.(\w+)',opts)
  priority=priority[1] if priority else 'NORMAL'
  canceled='true' if re.search(r'receiveCanceled\s*=\s*true',opts) else 'false'
  side='Client' if '/client/' in str(p) else 'Common'
  out[side].append(f'        GameEvents.BUS.addListener(DMZEvent.{event}.class, EventPriority.{priority}, {canceled}, {package[1]}.{p.stem}::{method});')
for side,lines in out.items():
 name=side+'CustomEventSubscribers'
 Path(f'src/main/java/com/dragonminez/platform/event/{name}.java').write_text(f'''package com.dragonminez.platform.event;

import com.dragonminez.common.events.DMZEvent;

/** Explicit subscriptions for the loader-independent DragonMineZ gameplay API. */
public final class {name} {{
    private static boolean installed;
    private {name}() {{}}
    public static synchronized void register() {{
        if (installed) return;
'''+ '\n'.join(lines)+'\n        installed = true;\n    }\n}\n')
 print(side,len(lines),'custom event subscribers')
