package com.dragonminez.platform.event.player;

import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class PlayerInteractEvent extends PlayerEvent {
    private final InteractionHand hand;
    private final BlockPos pos;
    private InteractionResult cancellationResult = InteractionResult.FAIL;
    protected PlayerInteractEvent(Player player, InteractionHand hand, BlockPos pos) {
        super(player); this.hand = hand; this.pos = pos;
    }
    public InteractionHand getHand() { return hand; }
    public BlockPos getPos() { return pos; }
    public Level getLevel() { return getEntity().level(); }
    public ItemStack getItemStack() { return getEntity().getItemInHand(hand); }
    public InteractionResult getCancellationResult() { return cancellationResult; }
    public void setCancellationResult(InteractionResult result) { cancellationResult = java.util.Objects.requireNonNull(result); }
    public InteractionResult result() { return isCanceled() ? cancellationResult : InteractionResult.PASS; }
    public static final class RightClickItem extends PlayerInteractEvent implements ICancellableEvent {
        public RightClickItem(Player player, InteractionHand hand) { super(player, hand, player.blockPosition()); }
    }
    public static final class RightClickBlock extends PlayerInteractEvent implements ICancellableEvent {
        private final BlockHitResult hit;
        public RightClickBlock(Player player, InteractionHand hand, BlockHitResult hit) { super(player, hand, hit.getBlockPos()); this.hit = hit; }
        public BlockHitResult getHitVec() { return hit; }
        public Direction getFace() { return hit.getDirection(); }
    }
    public static final class LeftClickBlock extends PlayerInteractEvent implements ICancellableEvent {
        private final Direction face;
        public LeftClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) { super(player, hand, pos); this.face = face; }
        public Direction getFace() { return face; }
    }
    public static final class EntityInteract extends PlayerInteractEvent implements ICancellableEvent {
        private final Entity target;
        public EntityInteract(Player player, InteractionHand hand, Entity target) { super(player, hand, target.blockPosition()); this.target = target; }
        public Entity getTarget() { return target; }
    }
    public static final class EntityInteractSpecific extends PlayerInteractEvent implements ICancellableEvent {
        private final Entity target;
        private final Vec3 hit;
        public EntityInteractSpecific(Player player, InteractionHand hand, Entity target, Vec3 hit) { super(player, hand, target.blockPosition()); this.target = target; this.hit = hit; }
        public Entity getTarget() { return target; }
        public Vec3 getLocalPos() { return hit.subtract(target.position()); }
    }
}
