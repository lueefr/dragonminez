import com.dragonminez.common.init.armor.ModArmorMaterials;
import com.dragonminez.common.init.armor.NativeArmorItem;
import com.dragonminez.platform.item.ArmorData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;

public final class ArmorMigrationTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var material = ModArmorMaterials.KIKONO.value();
        var chest = net.minecraft.core.component.DataComponentMap.builder()
                .set(DataComponents.ATTRIBUTE_MODIFIERS, material.createAttributes(ArmorType.CHESTPLATE))
                .set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(
                        ArmorType.CHESTPLATE.getSlot()).setAsset(material.assetId()).build())
                .set(DataComponents.MAX_DAMAGE, ArmorType.CHESTPLATE.getDurability(material.durability())).build();
        if (!ArmorData.isArmor(chest) || ArmorData.defense(chest) != 35) throw new AssertionError("Custom defense lost");
        if (chest.get(DataComponents.EQUIPPABLE).slot() != ArmorType.CHESTPLATE.getSlot()) throw new AssertionError("Wrong slot");
        if (!ArmorData.asset(chest).toString().equals("dragonminez:kikono")) throw new AssertionError("Wrong asset");
        if (chest.get(DataComponents.MAX_DAMAGE) != 1184) throw new AssertionError("Durability lost");
        var iron = net.minecraft.world.item.equipment.ArmorMaterials.IRON;
        var vanilla = net.minecraft.core.component.DataComponentMap.builder()
                .set(DataComponents.ATTRIBUTE_MODIFIERS, iron.createAttributes(ArmorType.CHESTPLATE))
                .set(DataComponents.EQUIPPABLE, chest.get(DataComponents.EQUIPPABLE)).build();
        if (!ArmorData.isArmor(vanilla) || ArmorData.defense(vanilla) != 6) throw new AssertionError("Vanilla template not recognized");
        var onlyEquippable = net.minecraft.core.component.DataComponentMap.builder()
                .set(DataComponents.EQUIPPABLE, chest.get(DataComponents.EQUIPPABLE)).build();
        if (ArmorData.isArmor(onlyEquippable)) throw new AssertionError("Non-armor equipment treated as armor");
        if (ArmorData.isArmor(net.minecraft.core.component.DataComponentMap.EMPTY)) throw new AssertionError("Ordinary item treated as armor");
        System.out.println("Armor migration: native component defense, equipment slot, asset, durability, vanilla template interoperability and non-armor exclusion passed.");
    }
}
