package com.dragonminez.platform.event.player;

import com.dragonminez.platform.event.Event;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;

/** Gameplay lifecycle notifications supplied by Fabric callbacks and targeted native hooks. */
public abstract class PlayerEvent extends Event {
    private final Player player;
    protected PlayerEvent(Player player) { this.player = java.util.Objects.requireNonNull(player); }
    public Player getEntity() { return player; }
    public static final class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player player) { super(player); }
    }
    public static final class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player player) { super(player); }
    }
    public static final class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;
        public PlayerRespawnEvent(Player player, boolean alive) { super(player); endConquered = alive; }
        public boolean isEndConquered() { return endConquered; }
    }
    public static final class PlayerChangedDimensionEvent extends PlayerEvent {
        private final ResourceKey<Level> from, to;
        public PlayerChangedDimensionEvent(Player player, ResourceKey<Level> from, ResourceKey<Level> to) {
            super(player); this.from = from; this.to = to;
        }
        public ResourceKey<Level> getFrom() { return from; }
        public ResourceKey<Level> getTo() { return to; }
    }
    public static final class PlayerChangeGameModeEvent extends PlayerEvent implements ICancellableEvent {
        private final GameType oldMode, newMode;
        public PlayerChangeGameModeEvent(Player player, GameType oldMode, GameType newMode) {
            super(player); this.oldMode = oldMode; this.newMode = newMode;
        }
        public GameType getCurrentGameMode() { return oldMode; }
        public GameType getNewGameMode() { return newMode; }
    }
    public static final class NameFormat extends PlayerEvent {
        private Component displayname;
        public NameFormat(Player player, Component original) { super(player); displayname = original; }
        public Component getDisplayname() { return displayname; }
        public void setDisplayname(Component name) { displayname = java.util.Objects.requireNonNull(name); }
    }
    public static final class TabListNameFormat extends PlayerEvent {
        private Component displayName;
        public TabListNameFormat(Player player, Component original) { super(player); displayName = original; }
        public Component getDisplayName() { return displayName; }
        public void setDisplayName(Component name) { displayName = name; }
    }
    public static final class ItemCraftedEvent extends PlayerEvent {
        private final ItemStack crafting;
        public ItemCraftedEvent(Player player, ItemStack stack) { super(player); crafting = stack; }
        public ItemStack getCrafting() { return crafting; }
    }
}
