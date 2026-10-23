from pathlib import Path
import re
scope='com.dragonminez.platform.registry.RegistrationScope'
for p in Path('src/main/java').rglob('*.java'):
 if '/platform/registry/' in str(p):continue
 old=p.read_text();s=old
 s=s.replace('net.neoforged.neoforge.registries.DeferredRegister','com.dragonminez.platform.registry.DeferredRegister').replace('net.neoforged.neoforge.registries.DeferredHolder','com.dragonminez.platform.registry.DeferredHolder')
 s=re.sub(r'\bregister\(IEventBus\s+\w+\)', 'register()',s)
 s=re.sub(r'\.register\((?:modEventBus|eventBus|bus)\)', '.register()',s)
 if not re.search(r'\bIEventBus\b',s.replace('import net.neoforged.bus.api.IEventBus;','')):s=s.replace('import net.neoforged.bus.api.IEventBus;\n','')
 s=s.replace('new Item.Properties()',scope+'.itemProperties()').replace('new net.minecraft.world.item.Item.Properties()',scope+'.itemProperties()')
 s=s.replace('BlockBehaviour.Properties.of()',scope+'.blockProperties()')
 if '/common/init/item/' in str(p):s=s.replace('new Properties()',scope+'.itemProperties()')
 # ResourceKey.location was renamed to identifier in 26.1.
 s=s.replace('.location()', '.identifier()')
 if p.name=='MainItems.java':
  s=s.replace('public static final Item.Properties properties = '+scope+'.itemProperties();','private static Item.Properties properties() { return '+scope+'.itemProperties(); }')
  s=s.replace('properties.stacksTo(', 'properties().stacksTo(')
 if old!=s:p.write_text(s)
