package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.menu.menutypes.FuelGeneratorMenu;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import com.dragonminez.common.init.menu.menutypes.PatternStationMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import com.dragonminez.platform.registry.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import com.dragonminez.platform.registry.DeferredHolder;

public class MainMenus {
	public static final DeferredRegister<MenuType<?>> MENUS =
			DeferredRegister.create(BuiltInRegistries.MENU, Reference.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<FuelGeneratorMenu>> FUEL_GENERATOR_MENU =
			registerMenuType("fuel_generator_menu", FuelGeneratorMenu::new);

	public static final DeferredHolder<MenuType<?>, MenuType<KikonoStationMenu>> KIKONO_STATION_MENU =
			registerMenuType("kikono_station_menu", KikonoStationMenu::new);

	public static final DeferredHolder<MenuType<?>, MenuType<PatternStationMenu>> PATTERN_STATION_MENU =
			registerMenuType("pattern_station_menu", PatternStationMenu::new);

	public static final DeferredHolder<MenuType<?>, MenuType<GravityDeviceMenu>> GRAVITY_DEVICE_MENU =
			registerMenuType("gravity_device_menu", GravityDeviceMenu::new);

	private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, ExtendedMenuType.ExtendedFactory<T, BlockPos> factory) {
		return MENUS.register(name, () -> new ExtendedMenuType<>(factory, BlockPos.STREAM_CODEC));
	}

	public static void register() {
		MENUS.register();
	}
}
