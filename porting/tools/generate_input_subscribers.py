from pathlib import Path
import re
lines=[]
for path in sorted(Path('src/main/java/com/dragonminez/client').rglob('*.java')):
 s=path.read_text();package=re.search(r'package ([\w.]+);',s)
 if not package:continue
 for m in re.finditer(r'@SubscribeEvent(?:\(([^)]*)\))?\s+public\s+static\s+void\s+(\w+)\s*\(\s*((?:InputEvent\.(?:Key|MouseScrollingEvent|InteractionKeyMappingTriggered)|ClientPlayerNetworkEvent\.(?:Clone|LoggingIn|LoggingOut)|ViewportEvent\.(?:ComputeFov|ComputeCameraAngles|ComputeFogColor|RenderFog)))\s+\w+\s*\)',s):
  options,method,event=m.groups();options=options or ''
  priority=re.search(r'priority\s*=\s*EventPriority\.(\w+)',options)
  canceled='true' if re.search(r'receiveCanceled\s*=\s*true',options) else 'false'
  lines.append(f'        GameEvents.BUS.addListener({event}.class, EventPriority.{priority[1] if priority else "NORMAL"}, {canceled}, {package[1]}.{path.stem}::{method});')
Path('src/main/java/com/dragonminez/platform/event/client/InputSubscribers.java').write_text('''package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.*;
public final class InputSubscribers {
    private static boolean installed;
    private InputSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
'''+ '\n'.join(lines)+'\n        installed = true;\n    }\n}\n')
print(len(lines),'input subscribers')
p=Path('src/main/java/com/dragonminez/client/DMZClient.java');s=p.read_text()
if 'InputSubscribers.register()' not in s:s=s.replace('com.dragonminez.platform.event.client.HudSubscribers.register();','com.dragonminez.platform.event.client.InputSubscribers.register();\n        com.dragonminez.platform.event.client.HudSubscribers.register();');p.write_text(s)
p=Path('build.gradle.kts');s=p.read_text().replace('exclude("CommonCustomEventSubscribers.java",', 'exclude("client/InputSubscribers.java", "CommonCustomEventSubscribers.java",');p.write_text(s)
