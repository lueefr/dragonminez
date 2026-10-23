"""Mechanical name migrations only; semantic API changes are handled separately."""
from pathlib import Path
import re
changes = {
 'software.bernie.geckolib': 'com.geckolib',
 'javax.annotation.Nullable': 'org.jetbrains.annotations.Nullable',
 'javax.annotation.Nonnull': 'org.jetbrains.annotations.NotNull',
}
identifiers = {'ResourceLocation': 'Identifier', 'ResourceLocationException': 'IdentifierException',
               'ResourceLocationArgument': 'IdentifierArgument', 'GuiGraphics': 'GuiGraphicsExtractor'}
for path in Path('src/main/java').rglob('*.java'):
 old=path.read_text(); text=old
 for a,b in changes.items(): text=text.replace(a,b)
 for a,b in identifiers.items(): text=re.sub(r'\b'+a+r'\b',b,text)
 text=text.replace('getGameProfile().getName()', 'getGameProfile().name()').replace('getGameProfile().getId()', 'getGameProfile().id()')
 if old!=text: path.write_text(text)
