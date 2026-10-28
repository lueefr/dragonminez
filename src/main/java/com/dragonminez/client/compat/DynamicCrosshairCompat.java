package com.dragonminez.client.compat;

import com.dragonminez.client.combat.ClientKiAim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/** Official register_api IMC protocol; no hard linkage or bundled optional-mod classes. */
@EventBusSubscriber(modid="dragonminez",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DynamicCrosshairCompat {
    private static boolean wasActive;
    private DynamicCrosshairCompat() {}
    @SubscribeEvent @SuppressWarnings({"unchecked","rawtypes"})
    public static void register(InterModEnqueueEvent event) {
        if(!net.neoforged.fml.ModList.get().isLoaded("dynamiccrosshair"))return;
        try {
            var api=Class.forName("mod.crend.dynamiccrosshairapi.DynamicCrosshairApi");
            var interaction=(Class<? extends Enum>)Class.forName("mod.crend.dynamiccrosshairapi.interaction.InteractionType");
            var crosshair=Class.forName("mod.crend.dynamiccrosshairapi.crosshair.Crosshair").getConstructor(interaction);
            Object plugin=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{
                return switch(method.getName()) {
                    case "getNamespace","getModId" -> "dragonminez";
                    case "forceCheck" -> true;
                    case "forceInvalidate" -> {boolean active=ClientKiAim.active(),changed=wasActive!=active;wasActive=active;yield active||changed;}
                    case "computeFromItem","overrideFromItem" -> ClientKiAim.active()?crosshair.newInstance(Enum.valueOf(interaction,ClientKiAim.rangedState())):null;
                    case "toString" -> "DragonMineZ Dynamic Crosshair integration";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> method.isDefault()?InvocationHandler.invokeDefault(proxy,method,args):null;
                };
            });
            net.neoforged.fml.InterModComms.sendTo("dynamiccrosshair","register_api",()->plugin);
        }catch(ReflectiveOperationException failure){com.dragonminez.LogUtil.warn(com.dragonminez.Env.CLIENT,"Dynamic Crosshair API unavailable: {}",failure.toString());}
    }
}
