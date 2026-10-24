package com.dragonminez.common.init.entities.questnpc;

import com.dragonminez.api.npc.CustomNpcRegistry;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenCustomNpcS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.*;

/** Separate entity type and definition source; built-in masters and quest NPCs are never rewritten. */
public final class CustomNpcEntity extends QuestNPCEntity {
    private static final EntityDataAccessor<String> DEFINITION = SynchedEntityData.defineId(CustomNpcEntity.class,EntityDataSerializers.STRING);
    private String cachedJson;
    private CustomNpcRegistry.Definition cachedDefinition;
    private long configRevision=-1;

    public CustomNpcEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type,level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);builder.define(DEFINITION,"");
    }
    public void setDefinition(CustomNpcRegistry.Definition definition) {
        setNpcId(definition.id());setMasterNpc(true);
        entityData.set(DEFINITION,CustomNpcRegistry.toJson(definition));
        setCustomName(Component.translatable(definition.name()));setCustomNameVisible(true);
        getPersistentData().putInt(NpcDispositionService.NPC_ALIGNMENT_TAG,definition.alignment());
    }
    public CustomNpcRegistry.Definition definition() {
        String json=entityData.get(DEFINITION);
        if (!json.equals(cachedJson)) {
            cachedJson=json;cachedDefinition=json.isEmpty()?null:CustomNpcRegistry.fromJson(json);
        }
        return cachedDefinition;
    }
    public net.minecraft.world.item.ItemStack cosmeticItem(net.minecraft.world.entity.EquipmentSlot slot) {
        var definition=definition();
        if (definition==null) return net.minecraft.world.item.ItemStack.EMPTY;
        String item=definition.armor().get(switch(slot) {
            case HEAD -> "head"; case CHEST -> "chest"; case LEGS -> "legs"; case FEET -> "feet"; default -> "";
        });
        return item==null ? net.minecraft.world.item.ItemStack.EMPTY
                : new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(item)));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && configRevision!=CustomNpcRegistry.INSTANCE.revision()) {
            configRevision=CustomNpcRegistry.INSTANCE.revision();
            CustomNpcRegistry.INSTANCE.get(getNpcId()).ifPresent(this::setDefinition);
        }
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"custom_idle",5,event -> event.setAndContinue(RawAnimation.begin().thenLoop("base.idle"))));
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && CustomNpcRegistry.INSTANCE.get(getNpcId()).isPresent()) {
            NetworkHandler.sendToPlayer(new OpenCustomNpcS2C(getId()),serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putString("CustomNpcDefinition",entityData.get(DEFINITION));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CustomNpcDefinition")) entityData.set(DEFINITION,tag.getString("CustomNpcDefinition"));
    }
}
