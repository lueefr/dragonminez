package com.dragonminez.platform.rules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;

public final class RuleValues {
    private RuleValues() {}
    /** Gameplay checks use the owning server level; disconnected client previews use defaults. */
    public static <T> T get(Level level, GameRule<T> rule) {
        if (level instanceof ServerLevel serverLevel) return serverLevel.getGameRules().get(rule);
        var server = level.getServer();
        if (server != null) {
            var serverLevel = server.getLevel(level.dimension());
            if (serverLevel != null) return serverLevel.getGameRules().get(rule);
        }
        return rule.defaultValue();
    }
}
