from pathlib import Path
import re
# Receivers in this source tree are entity identifiers; leave compound expressions for review.
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();orig=s
 s=re.sub(r'\b((?:this\.)?[A-Za-z_]\w*)\.getPersistentData\(\)',r'com.dragonminez.platform.entity.EntityData.get(\1)',s)
 if s!=orig:p.write_text(s)
