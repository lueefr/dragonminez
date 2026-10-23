from pathlib import Path
import re

for path in Path('src/main/java/com/dragonminez/common/init/block/entity').glob('*.java'):
    source = path.read_text()
    pattern = r'protected void (saveAdditional|loadAdditional)\(CompoundTag (\w+), (?:net\.minecraft\.core\.)?HolderLookup\.Provider registries\)\s*\{'
    edits = []
    for match in re.finditer(pattern, source):
        depth = 1
        end = match.end()
        while depth:
            if source[end] == '{': depth += 1
            elif source[end] == '}': depth -= 1
            end += 1
        method, tag = match.groups()
        body = source[match.end():end-1]
        saving = method == 'saveAdditional'
        native = 'ValueOutput' if saving else 'ValueInput'
        param = 'output' if saving else 'input'
        body = body.replace(f'super.{method}({tag}, registries);', f'super.{method}({param});')
        if saving:
            body = body.replace(f'{tag}.put("inventory", itemHandler.serializeNBT(registries));', 'itemHandler.writeValue(output.child("inventory"));')
            replacement = f'protected void {method}(net.minecraft.world.level.storage.{native} {param}) {{\n        CompoundTag {tag} = new CompoundTag();' + body + f'\n        com.dragonminez.platform.nbt.NbtValues.write(output, {tag});\n    }}'
        else:
            body = body.replace(f'itemHandler.deserializeNBT(registries, {tag}.getCompoundOrEmpty("inventory"));', 'itemHandler.readValue(input.childOrEmpty("inventory"));')
            replacement = f'protected void {method}(net.minecraft.world.level.storage.{native} {param}) {{\n        CompoundTag {tag} = com.dragonminez.platform.nbt.NbtValues.read(input);' + body + '}'
        edits.append((match.start(), end, replacement))
    for start, end, replacement in reversed(edits): source = source[:start] + replacement + source[end:]
    if edits:
        path.write_text(source)
        print(path.name, len(edits))
