package com.dragonminez.common.combat.aim;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.racial.impl.GlindDivinity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class KiDetection {
    private KiDetection() {}
    public static boolean visible(StatsData observer,LivingEntity entity,String channel) {
        if(!(entity instanceof Player player))return true;
        var target=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        return target==null||allows(observer,target,channel);
    }
    public static boolean allows(StatsData observer,StatsData target,String channel) {
        if(target==null)return true;
        boolean nativeSight=!target.getStatus().isAndroidUpgraded()&&!GlindDivinity.isHiddenFrom(target,observer)
                &&!(TransformationsHelper.hasGodFormActive(target)&&!TransformationsHelper.hasGodKi(observer));
        if(channel.equals("scouter"))nativeSight=!GlindDivinity.isHiddenFrom(target,observer);
        else if(!channel.equals("instant_transmission")) {
            nativeSight&=target.getResources().getPowerRelease()>0;
            if(target.getPlayer() instanceof Player player)nativeSight&=!TransformationsHelper.hasAntiKiCloak(player);
        }
        return ConfigManager.getAttributes().detects(observer,target,channel,nativeSight);
    }
}
