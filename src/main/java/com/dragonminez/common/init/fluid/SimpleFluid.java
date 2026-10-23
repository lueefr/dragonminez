package com.dragonminez.common.init.fluid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.*;
import java.util.function.Supplier;

/** A finite, water-like fluid pair using the native fluid simulation. */
public final class SimpleFluid extends WaterFluid {
    private final boolean source;
    private final int tint;
    private final Supplier<? extends Fluid> still;
    private final Supplier<? extends Fluid> flowing;
    private final Supplier<? extends LiquidBlock> block;
    private final Supplier<? extends Item> bucket;

    public SimpleFluid(boolean source, int color, Supplier<? extends Fluid> still,
            Supplier<? extends Fluid> flowing, Supplier<? extends LiquidBlock> block, Supplier<? extends Item> bucket) {
        this.source = source;
        this.tint = 0xA1000000 | color;
        this.still = still;
        this.flowing = flowing;
        this.block = block;
        this.bucket = bucket;
        registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7).setValue(FALLING, false));
    }
    @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(LEVEL);
    }
    @Override public Fluid getSource() { return still.get(); }
    @Override public Fluid getFlowing() { return flowing.get(); }
    @Override public Item getBucket() { return bucket.get(); }
    @Override public boolean isSource(FluidState state) { return source; }
    @Override public int getAmount(FluidState state) { return source ? 8 : state.getValue(LEVEL); }
    @Override public boolean isSame(Fluid fluid) { return fluid == still.get() || fluid == flowing.get(); }
    @Override protected boolean canConvertToSource(ServerLevel level) { return false; }
    @Override public BlockState createLegacyBlock(FluidState state) {
        return block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }
    public int tintColor() { return tint; }
}
