from pathlib import Path
import re
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();old=s
 if 'com.geckolib.animation.AnimationState' in s or re.search(r'\bAnimationState\s*<',s):
  s=s.replace('import com.geckolib.animation.AnimationState;','import com.geckolib.animation.state.AnimationTest;')
  s=re.sub(r'\bAnimationState\b','AnimationTest',s)
  if 'import com.geckolib.animation.state.AnimationTest;' not in s:
   s=s.replace('\n\nimport ', '\n\nimport com.geckolib.animation.state.AnimationTest;\nimport ',1)
  variables=set(re.findall(r'AnimationTest\s*<[^\n]+?>\s+(\w+)',s))
  for v in variables:
   s=s.replace(v+'.getAnimatable()',v+'.animatable()').replace(v+'.getController()',v+'.controller()')
   s=s.replace(v+'.getPartialTick()',v+'.renderState().getPartialTick()').replace(v+'.getAnimationTick()',v+'.renderState().getAnimatableAge()')
   s=s.replace(v+'.setData(',v+'.renderState().addGeckolibData(')
 s=re.sub(r'(new AnimationController\s*<[^>]*>\s*\()this\s*,\s*',r'\1',s)
 if s!=old:p.write_text(s)
