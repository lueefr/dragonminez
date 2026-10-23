package com.dragonminez.platform.event;

import net.fabricmc.api.EnvType;
import java.lang.annotation.*;

/** Registration metadata for the explicit handler manifest; does not scan the classpath. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    enum Bus { GAME, MOD }
    String modid();
    Bus bus() default Bus.GAME;
    EnvType[] value() default {EnvType.CLIENT, EnvType.SERVER};
}
