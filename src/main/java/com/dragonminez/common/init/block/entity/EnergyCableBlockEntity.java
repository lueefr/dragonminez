package com.dragonminez.common.init.block.entity;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.server.energy.StarEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import com.dragonminez.server.energy.EnergyTransfers;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.RenderUtil;

public class EnergyCableBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(150, 15) {
		@Override
		public void onEnergyChanged() { setChanged(); }
	};

	public EnergyCableBlockEntity(BlockPos pPos, BlockState pState) {
		super(MainBlockEntities.ENERGY_CABLE_BE.get(), pPos, pState);
	}

	public StarEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("energy", input.getIntOr("energy", 0));
        energyStorage.loadNBT(tag);
    }

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel == null || pLevel.isClientSide()) return;
		if (energyStorage.getEnergyStored() <= 0) return;

		int toDistribute = energyStorage.getEnergyStored();
		int distributed = 0;

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pPos.relative(dir);
			BlockEntity be = pLevel.getBlockEntity(neighborPos);
			if (be != null && !(be instanceof EnergyCableBlockEntity) && isDMZBlock(pLevel.getBlockState(neighborPos).getBlock())) {
				distributed += pushTo(pLevel, neighborPos, dir.getOpposite(), toDistribute - distributed);
				if (distributed >= toDistribute) break;
			}
		}

		if (distributed < toDistribute) {
			for (Direction dir : Direction.values()) {
				BlockEntity be = pLevel.getBlockEntity(pPos.relative(dir));
				if (be instanceof EnergyCableBlockEntity) {
					distributed += pushTo(pLevel, pPos.relative(dir), dir.getOpposite(), toDistribute - distributed);
					if (distributed >= toDistribute) break;
				}
			}
		}

	}

	private boolean isDMZBlock(Block block) {
		var key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null && key.getNamespace().equals(Reference.MOD_ID);
	}

	private int pushTo(Level level, BlockPos pos, Direction side, int amount) {
		EnergyStorage energy = EnergyStorage.SIDED.find(level, pos, side);
		if (energy == null) return 0;
		return EnergyTransfers.move(energyStorage, energy, amount);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>("controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationTest<T> tAnimationState) {
		return tAnimationState.setAndContinue(RawAnimation.begin().then("animation.energy_cable.idle", Animation.LoopType.LOOP));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtil.getCurrentTick();
	}

}
