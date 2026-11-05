package com.dragonminez.api.runtime;

import java.util.Map;

/** Transient attack phases expose numeric queries without changing native attack timing. */
public final class AttackState {
    public enum Phase { IDLE, CHARGING, ACTIVE, RECOVERY }
    private Phase phase = Phase.IDLE;
    private String technique = "";
    private int ticks, active = 1, recovery;
    private double charge;
    public Phase phase() { return phase; }
    public String technique() { return technique; }
    public void start(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Attack needs a technique ID");
        technique = id; phase = Phase.CHARGING; ticks = 0; charge = 0;
    }
    public void release(String id, double percent, int duration, int rest) {
        if (!Double.isFinite(percent) || percent < 0 || percent > 200 || duration < 1 || rest < 0) throw new IllegalArgumentException("Invalid attack phase timing/charge");
        start(id); charge = percent; active = duration; recovery = rest; phase = Phase.ACTIVE;
    }
    public void cancel() { phase = Phase.IDLE; technique = ""; ticks = 0; charge = 0; }
    public boolean tick(boolean charging, String id, double percent) {
        if (phase == Phase.CHARGING) {
            if (!charging || !technique.equals(id)) { cancel(); return true; }
            if (!Double.isFinite(percent)) throw new IllegalArgumentException("Attack charge must be finite");
            charge = Math.max(0, Math.min(200, percent)); ticks = Math.min(Integer.MAX_VALUE - 1, ticks) + 1;
        } else if (phase == Phase.ACTIVE || phase == Phase.RECOVERY) {
            ticks++;
            if (ticks >= (phase == Phase.ACTIVE ? active : recovery)) {
                if (phase == Phase.ACTIVE && recovery > 0) { phase = Phase.RECOVERY; ticks = 0; }
                else cancel();
                return true;
            }
        }
        return false;
    }
    public Map<String, Double> numbers() {
        return Map.of("attack.charging", phase == Phase.CHARGING ? 1d : 0d,
                "attack.active", phase == Phase.ACTIVE ? 1d : 0d,
                "attack.recovery", phase == Phase.RECOVERY ? 1d : 0d,
                "attack.ticks", (double) ticks, "attack.charge", charge);
    }
    public void view(Map<String, Double> values) {
        cancel();
        if (values.getOrDefault("attack.charging", 0d) == 1) phase = Phase.CHARGING;
        else if (values.getOrDefault("attack.active", 0d) == 1) phase = Phase.ACTIVE;
        else if (values.getOrDefault("attack.recovery", 0d) == 1) phase = Phase.RECOVERY;
        ticks = (int) Math.max(0, Math.min(Integer.MAX_VALUE, values.getOrDefault("attack.ticks", 0d)));
        charge = Math.max(0, Math.min(200, values.getOrDefault("attack.charge", 0d)));
    }
}
