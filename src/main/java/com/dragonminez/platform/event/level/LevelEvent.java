package com.dragonminez.platform.event.level;
import com.dragonminez.platform.event.Event;
import net.minecraft.world.level.Level;
public abstract class LevelEvent extends Event {
    private final Level level;
    protected LevelEvent(Level level) { this.level = level; }
    public Level getLevel() { return level; }
    public static final class Load extends LevelEvent { public Load(Level level) { super(level); } }
    public static final class Unload extends LevelEvent { public Unload(Level level) { super(level); } }
}
