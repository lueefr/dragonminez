package com.dragonminez.platform.event.level;
import com.dragonminez.platform.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.Level;
import java.util.List;
public abstract class ExplosionEvent extends Event {
    private final ServerExplosion explosion;
    private ExplosionEvent(ServerExplosion explosion) { this.explosion = explosion; }
    public ServerExplosion getExplosion() { return explosion; }
    public Level getLevel() { return explosion.level(); }
    public static final class Detonate extends ExplosionEvent {
        private final List<BlockPos> blocks;
        public Detonate(ServerExplosion explosion, List<BlockPos> blocks) { super(explosion); this.blocks = blocks; }
        public List<BlockPos> getAffectedBlocks() { return blocks; }
    }
}
