from pathlib import Path
import re
loader='net.fabricmc.loader.api.FabricLoader.getInstance()'
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();old=s
 if 'import net.neoforged.api.distmarker.Dist;' in s:
  s=s.replace('import net.neoforged.api.distmarker.Dist;','import net.fabricmc.api.EnvType;')
  s=re.sub(r'\bDist\b','EnvType',s).replace('EnvType.DEDICATED_SERVER','EnvType.SERVER')
 if 'import net.neoforged.api.distmarker.OnlyIn;' in s:
  s=s.replace('import net.neoforged.api.distmarker.OnlyIn;','import net.fabricmc.api.Environment;').replace('@OnlyIn(', '@Environment(')
 s=s.replace('import net.neoforged.fml.loading.FMLEnvironment;\n','').replace('FMLEnvironment.dist',loader+'.getEnvironmentType()')
 s=s.replace('import net.neoforged.fml.loading.FMLPaths;\n','').replace('FMLPaths.CONFIGDIR.get()',loader+'.getConfigDir()').replace('FMLPaths.GAMEDIR.get()',loader+'.getGameDir()')
 if 'ModList.get().isLoaded(' in s:
  s=s.replace('ModList.get().isLoaded(',loader+'.isModLoaded(')
  if 'ModList.' not in s:s=s.replace('import net.neoforged.fml.ModList;\n','')
 if s!=old:p.write_text(s)
