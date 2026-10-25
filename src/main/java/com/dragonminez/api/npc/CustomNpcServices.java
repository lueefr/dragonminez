package com.dragonminez.api.npc;

import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class CustomNpcServices {
    private CustomNpcServices() {}
    /** Server-owned definition, range, alignment, price and inventory are rechecked for every sale. */
    public static boolean purchase(ServerPlayer player, CustomNpcEntity npc, int offerIndex) {
        if (!player.serverLevel().getServer().isSameThread()) throw new IllegalStateException("NPC services require the server thread");
        if (!player.isAlive() || player.isSpectator() || StorageManager.isLoadPending(player)
                || npc.level()!=player.level() || npc.distanceToSqr(player)>100 || !npc.isAlive()) return false;
        var definition=CustomNpcRegistry.INSTANCE.get(npc.getNpcId()).orElse(null);
        if (definition==null || offerIndex<0 || offerIndex>=definition.offers().shop().size()
                || NpcDispositionService.getServiceBlocker(player,npc.getNpcId())!=null) return false;
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if (data==null || data.getStatus().isStunned() || data.getStatus().isFused()
                || data.getStatus().getFusionPartnerUUID()!=null) return false;
        var offer=definition.offers().shop().get(offerIndex);
        var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(offer.item())).orElse(null);
        if (item==null || data.getResources().getTrainingPointsExact()<offer.price()) return false;
        ItemStack stack=new ItemStack(item,offer.count());
        if (stack.isEmpty() || offer.count()>stack.getMaxStackSize()) return false;
        var simulated=new SimpleContainer(36);
        for (int slot=0;slot<36;slot++) simulated.setItem(slot,player.getInventory().getItem(slot).copy());
        if (!simulated.addItem(stack.copy()).isEmpty()) return false;
        data.getResources().setTrainingPointsExact(data.getResources().getTrainingPointsExact() - offer.price());
        player.getInventory().add(stack);
        player.inventoryMenu.broadcastChanges();
        StatsSyncS2C.sendRequested(player);
        return true;
    }
}
