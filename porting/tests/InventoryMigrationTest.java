import com.dragonminez.platform.inventory.ItemStackHandler;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class InventoryMigrationTest {
    static final class Inventory extends ItemStackHandler {
        int changes;
        Inventory() { super(3); }
        @Override protected void onContentsChanged(int slot) { changes++; }
        @Override public int getSlotLimit(int slot) { return 16; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot != 2; }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var prototype = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                .set(DataComponents.MAX_STACK_SIZE, 64).build();
        Items.STICK.builtInRegistryHolder().bindComponents(prototype);
        var inventory = new Inventory();
        var stack = new ItemStack(Items.STICK, 20);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Inventory fixture"));
        check(inventory.insertItem(0, stack, true).getCount() == 4, "Simulation ignores slot limit");
        check(inventory.isEmpty() && inventory.changes == 0, "Simulation mutated inventory");
        check(inventory.insertItem(0, stack, false).getCount() == 4, "Incorrect remainder");
        check(stack.getCount() == 20 && inventory.getItem(0).getCount() == 16, "Insertion mutated caller stack");
        check(inventory.insertItem(2, stack, false) == stack, "Rejected slot accepted item");
        var different = stack.copy(); different.remove(DataComponents.CUSTOM_NAME);
        check(inventory.insertItem(0, different, false) == different, "Different components merged");
        check(inventory.extractItem(0, 7, true).getCount() == 7 && inventory.getItem(0).getCount() == 16
                && inventory.changes == 1, "Extraction simulation mutated inventory");
        check(inventory.extractItem(0, 7, false).getCount() == 7 && inventory.getItem(0).getCount() == 9,
                "Incorrect extraction");
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var saved = inventory.serializeNBT(registries);
        check(saved.contains("Size") && saved.contains("Items"), "Legacy inventory keys changed");
        var restored = new Inventory(); restored.deserializeNBT(registries, saved);
        check(ItemStack.matches(inventory.getItem(0), restored.getItem(0)), "Item components failed round trip");
        check(restored.getSlots() == 3 && restored.getItem(1).isEmpty(), "Empty slots failed round trip");
        check(restored.extractItem(0, 99, false).getCount() == 9 && restored.isEmpty(), "Full extraction failed");
        System.out.println("Inventory: simulation, rejection, limits, caller ownership and component persistence passed.");
    }
}
