package com.dragonminez.common.config;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Set;

public final class RebirthConfig {
    public boolean enabled = true;
    public JsonObject requirements = new JsonObject();
    public Retention retention = new Retention();
    public Bonus bonus = new Bonus();
    public static final class Retention {
        public int statsPercent;
        public boolean skills, mastery;
        public boolean inventory = true, content = true, raceHistory = true;
        public boolean story, techniques, effects, cooldowns, bonusStats, history, alignment;
        public int tpPercent, pendingPercent;
    }
    public static final class Bonus {
        public double multiplier = 2;
        public long durationTicks = -1;
        public Cap cap = new Cap();
        public Stacking stacking = new Stacking();
    }
    public static final class Cap {
        public boolean enabled = true;
        public String mode = "previous_level";
        public int level = 100;
        public double tp = 200000;
    }
    public static final class Stacking {
        public int limit = 1;
        public String mode = "multiply", renewal = "restart", durationRenewal = "restart";
    }
    public static RebirthConfig parse(String json) {
        var config = new Gson().fromJson(json,RebirthConfig.class);
        if(config==null||config.requirements==null||config.retention==null||config.bonus==null||config.bonus.cap==null||config.bonus.stacking==null)
            throw new IllegalArgumentException("Missing reincarnation section");
        var bonus=config.bonus;var cap=bonus.cap;var stacking=bonus.stacking;
        if(!Double.isFinite(bonus.multiplier)||bonus.multiplier<=0||bonus.durationTicks < -1)
            throw new IllegalArgumentException("Invalid reincarnation multiplier or duration");
        if(!Set.of("previous_level","level","tp").contains(cap.mode)||cap.level<1||!Double.isFinite(cap.tp)||cap.tp<0)
            throw new IllegalArgumentException("Invalid reincarnation TP cap");
        if(stacking.limit<0||!Set.of("multiply","add","maximum").contains(stacking.mode)||!Set.of("restart","keep","extend").contains(stacking.renewal)||!Set.of("restart","keep","extend").contains(stacking.durationRenewal))
            throw new IllegalArgumentException("Invalid reincarnation stacking policy");
        if(config.retention.statsPercent<0||config.retention.statsPercent>100||config.retention.tpPercent<0||config.retention.tpPercent>100||config.retention.pendingPercent<0||config.retention.pendingPercent>100)throw new IllegalArgumentException("Retention percentage must be within 0..100");
        com.dragonminez.api.rules.Conditions.validate(config.requirements);
        return config;
    }
    public static RebirthConfig load(Path file) throws IOException {
        if(!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try(var input=RebirthConfig.class.getResourceAsStream("/data/dragonminez/defaults/progression/reincarnation.json")) {
                if(input==null)throw new IOException("Missing reincarnation defaults");
                Files.copy(input,file);
            }
        }
        if(Files.size(file)>1048576)throw new IllegalArgumentException("Reincarnation configuration exceeds 1 MiB");
        return parse(Files.readString(file,StandardCharsets.UTF_8));
    }
}
