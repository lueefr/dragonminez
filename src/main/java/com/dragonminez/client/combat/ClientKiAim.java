package com.dragonminez.client.combat;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.KiAimC2S;
import com.dragonminez.common.network.C2S.TechniqueChargeC2S;
import com.dragonminez.client.events.LockOnEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="dragonminez",value=Dist.CLIENT)
public final class ClientKiAim {
    private static int visibleUntil;
    private static net.minecraft.client.player.LocalPlayer observed;
    private ClientKiAim() {}
    public static boolean active() {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player!=observed){observed=player;visibleUntil=0;}
        if(player==null||player.isSpectator()||!player.isAlive())return false;
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if(data==null||!data.getStatus().isHasCreatedCharacter())return false;
        var techniques=data.getTechniques();
        return techniques.isTechniqueCharging()||techniques.isTechniqueChargeActive()
            ||player.tickCount<visibleUntil
            ||player.getMainHandItem().isEmpty()&&data.getSkills().getSkillLevel("kicontrol")>0
                &&(techniques.getSelectedTechnique() instanceof KiAttackData||com.dragonminez.client.util.KeyBinds.DESCEND.isDown());
    }
    public static String rangedState() {
        var player=Minecraft.getInstance().player;if(player==null)return "RANGED_WEAPON";
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if(data!=null&&(data.getTechniques().isTechniqueCharging()||data.getTechniques().isTechniqueChargeActive()))
            return data.getTechniques().getTechniqueChargePercent()>=100?"RANGED_WEAPON_CHARGED":"RANGED_WEAPON_CHARGING";
        return "RANGED_WEAPON";
    }
    public static void sendNow() {sendRay(true);}
    private static void sendRay(boolean reveal) {
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(observed!=mc.player){observed=mc.player;visibleUntil=0;}
        var camera=mc.gameRenderer.getMainCamera();Vec3 direction=new Vec3(camera.getLookVector()).normalize();
        var locked=LockOnEvent.getLockedTarget();
        NetworkHandler.sendToServer(new KiAimC2S(camera.getPosition(),direction,locked==null?-1:locked.getId()));
        if(reveal)visibleUntil=mc.player.tickCount+12;
    }
    public static void startCharge(int slot,int target) {sendNow();NetworkHandler.sendToServer(TechniqueChargeC2S.start(slot,target));}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();if(mc.player==null){visibleUntil=0;observed=null;return;}
        if(mc.screen==null&&active()&&(mc.player.tickCount&1)==0)sendRay(false);
    }
}
