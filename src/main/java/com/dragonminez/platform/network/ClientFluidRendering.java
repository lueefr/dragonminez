package com.dragonminez.platform.network;

import com.dragonminez.common.init.MainFluids;
import com.dragonminez.common.init.fluid.SimpleFluid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;

@Environment(EnvType.CLIENT)
public final class ClientFluidRendering {
    private ClientFluidRendering() {}
    public static void register() {
        register(MainFluids.SOURCE_HEALING.get(), MainFluids.FLOWING_HEALING.get());
        register(MainFluids.SOURCE_NAMEK.get(), MainFluids.FLOWING_NAMEK.get());
    }
    private static void register(FlowingFluid source, FlowingFluid flowing) {
        int color = ((SimpleFluid) source).tintColor();
        var model = new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still"), true),
                new Material(Identifier.withDefaultNamespace("block/water_flow"), true),
                new Material(Identifier.withDefaultNamespace("block/water_overlay"), true), state -> color);
        FluidRenderingRegistry.register(source, flowing, model);
    }
}
