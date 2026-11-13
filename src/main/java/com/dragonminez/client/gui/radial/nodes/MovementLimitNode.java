package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.SetMovementLimitC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Uses the flight radial's click, reset, percentage panel and wheel interaction. */
public final class MovementLimitNode extends FlightSpeedNode {
    private final boolean jump;
    public MovementLimitNode(boolean jump) { this.jump = jump; }
    @Override protected int currentFlightSpeed(StatsData stats) {
        return jump ? stats.getResources().getJumpLimit() : stats.getResources().getSprintLimit();
    }
    @Override public Component label(StatsData stats) {
        return Component.translatable(jump ? "gui.dragonminez.radial.jump_limit" : "gui.dragonminez.radial.sprint_limit");
    }
    @Override public boolean visible(StatsData stats) { return stats.getSkills().hasSkill(jump ? "jump" : "sprint"); }
    private void send(int value) { NetworkHandler.sendToServer(new SetMovementLimitC2S(jump, value)); }
    @Override public void onSelect(StatsData stats) { send(100); playToggle(false); }
    @Override public void adjustByWheel(StatsData stats, double delta) {
        if (delta == 0) return;
        int value = Math.max(5, Math.min(100, currentFlightSpeed(stats) + (delta > 0 ? 5 : -5)));
        if (value != currentFlightSpeed(stats)) { send(value); playClick(); }
    }
    @Override public List<RadialNode> buildOptions() {
        List<RadialNode> nodes = new ArrayList<>();
        for (int percent = 100; percent >= 5; percent -= 5) {
            final int value = percent;
            nodes.add(new AbstractRadialNode() {
                @Override public Component label(StatsData stats) { return Component.translatable("text.dragonminez.flightspeedlimitoptionnode.eaf8695fa7", value); }
                @Override public ResourceLocation icon(StatsData stats) { return null; }
                @Override public boolean active(StatsData stats) { return currentFlightSpeed(stats) == value; }
                @Override public int labelColor(StatsData stats) { return active(stats) ? GREEN : 0xFFFFFF; }
                @Override public void onSelect(StatsData stats) { send(value); playClick(); }
            });
        }
        return nodes;
    }
}
