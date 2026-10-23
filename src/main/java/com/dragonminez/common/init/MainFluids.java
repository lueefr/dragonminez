package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.fluid.SimpleFluid;
import com.dragonminez.platform.registry.DeferredHolder;
import com.dragonminez.platform.registry.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

public final class MainFluids {
    public static final DeferredRegister<Fluid> FLUIDS_REGISTER = DeferredRegister.create(BuiltInRegistries.FLUID, Reference.MOD_ID);
    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_HEALING = FLUIDS_REGISTER.register("healing_fluid", () -> healing(true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_HEALING = FLUIDS_REGISTER.register("flowing_healing_fluid", () -> healing(false));
    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_NAMEK = FLUIDS_REGISTER.register("namek_water_fluid", () -> namek(true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_NAMEK = FLUIDS_REGISTER.register("flowing_namek_water_fluid", () -> namek(false));
    private static SimpleFluid healing(boolean source) {
        return new SimpleFluid(source, 0xa4c977, SOURCE_HEALING, FLOWING_HEALING,
                () -> MainBlocks.HEALING_LIQUID.get(), () -> MainItems.HEALING_BUCKET.get());
    }
    private static SimpleFluid namek(boolean source) {
        return new SimpleFluid(source, 0xaef359, SOURCE_NAMEK, FLOWING_NAMEK,
                () -> MainBlocks.NAMEK_WATER_LIQUID.get(), () -> MainItems.NAMEK_WATER_BUCKET.get());
    }
    public static void register() { FLUIDS_REGISTER.register(); }
    private MainFluids() {}
}
