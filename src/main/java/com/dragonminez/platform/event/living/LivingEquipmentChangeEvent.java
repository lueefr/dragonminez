package com.dragonminez.platform.event.living;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
public final class LivingEquipmentChangeEvent extends LivingEvent {
    private final EquipmentSlot slot;
    private final ItemStack from, to;
    public LivingEquipmentChangeEvent(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to) {
        super(entity); this.slot = slot; this.from = from; this.to = to;
    }
    public EquipmentSlot getSlot() { return slot; }
    public ItemStack getFrom() { return from; }
    public ItemStack getTo() { return to; }
}
