from pathlib import Path
import re
for p in Path('src/main/java/com/dragonminez/client/init/entities/model').rglob('*.java'):
 s=p.read_text()
 m=re.search(r'\s*@Override\s+public void setCustomAnimations\([^\n]+\)\s*\{',s)
 if not m:continue
 start=m.start();brace=s.index('{',m.start());depth=1;i=brace+1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 body=s[brace+1:i-1]
 if 'getAnimationProcessor().getBone("head")' not in body:raise ValueError(str(p))
 replacement='\n'
 if 'GumBindEntity.isClientBound' in body:
  replacement+='\n    @Override protected boolean trackHead(T animatable) { return !GumBindEntity.isClientBound(animatable); }\n'
 if 'Mth.clamp' in body:
  replacement+='\n    @Override protected float pitchLimit() { return 20; }\n    @Override protected float yawLimit() { return 35; }\n    @Override protected boolean additiveHeadRotation() { return true; }\n'
 s=s[:start]+replacement+s[i:]
 s=s.replace('extends ExtractedGeoModel<', 'extends HeadTrackingGeoModel<').replace('import com.dragonminez.platform.client.model.ExtractedGeoModel;', 'import com.dragonminez.platform.client.model.HeadTrackingGeoModel;').replace('import com.geckolib.model.data.EntityModelData;\n','')
 p.write_text(s)
for p in Path('src/main/java').rglob('*.java'):
 if '/platform/' in str(p):continue
 s=p.read_text();orig=s
 s=re.sub(r'extends GeoEntityRenderer<([^<>,]+), net\.minecraft\.client\.renderer\.entity\.state\.LivingEntityRenderState>',r'extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<\1>',s)
 if s!=orig:p.write_text(s)
