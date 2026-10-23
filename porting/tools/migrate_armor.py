from pathlib import Path
root=Path('src/main/java')
for p in root.rglob('*.java'):
 s=p.read_text()
 if 'ArmorItem.Type' in s:
  s=s.replace('ArmorItem.Type','ArmorType')
  if 'import net.minecraft.world.item.equipment.ArmorType;' not in s:s=s.replace('\nimport ', '\nimport net.minecraft.world.item.equipment.ArmorType;\nimport ',1)
 s=s.replace('import net.minecraft.world.item.ArmorMaterial;', 'import net.minecraft.world.item.equipment.ArmorMaterial;')
 if p.name in ('DbzArmorItem.java','DbzArmorCapeItem.java','DbzArmorPatrollerItem.java'):
  s=s.replace('extends ArmorItem','extends NativeArmorItem').replace(' Type pType',' ArmorType pType')
  s=s.replace('import net.minecraft.world.item.ArmorItem;', 'import net.minecraft.world.item.equipment.ArmorType;')
 if 'instanceof ArmorItem' in s:
  s=s.replace('import net.minecraft.world.item.ArmorItem;', 'import com.dragonminez.platform.item.ArmorData;')
  for var in ('stack','newStack','itemStack'):
   s=s.replace(f'{var}.getItem() instanceof ArmorItem armor',f'ArmorData.isArmor({var})')
   s=s.replace(f'{var}.getItem() instanceof ArmorItem',f'ArmorData.isArmor({var})')
  s=s.replace('((ItemStack) (Object) this).getItem() instanceof ArmorItem','ArmorData.isArmor((ItemStack) (Object) this)')
  s=s.replace('armor.getDefense()', 'ArmorData.defense(stack)')
 p.write_text(s)
