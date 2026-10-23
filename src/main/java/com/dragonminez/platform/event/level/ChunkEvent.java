package com.dragonminez.platform.event.level;
import com.dragonminez.platform.event.Event;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
public abstract class ChunkEvent extends Event {
    private final Level level;
    private final ChunkAccess chunk;
    protected ChunkEvent(Level level, ChunkAccess chunk) { this.level = level; this.chunk = chunk; }
    public Level getLevel() { return level; }
    public ChunkAccess getChunk() { return chunk; }
    public static final class Load extends ChunkEvent {
        public Load(Level level, ChunkAccess chunk) { super(level, chunk); }
    }
}
