package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDataPackResources;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.*;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.init.entities.demon.DemonVillagersEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.server.world.gen.OverworldSurfaceRules;
import com.dragonminez.server.world.region.OverworldRegion;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

import com.dragonminez.platform.resource.AddPackFindersEvent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.registry.DeferredHolder;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.platform.event.EventBusSubscriber;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.SERVER_DATA) {
			event.addRepositorySource((packConsumer) -> {
				Pack dragonballPack = Pack.readMetaAndCreate(
					new net.minecraft.server.packs.PackLocationInfo("dmz_dragonballs_runtime_data", Component.literal("DMZ Dragonballs Runtime Data"), PackSource.BUILT_IN, java.util.Optional.empty()),
					new Pack.ResourcesSupplier() {
						@Override public net.minecraft.server.packs.PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo location) { return new DragonBallDataPackResources(location); }
						@Override public net.minecraft.server.packs.PackResources openFull(net.minecraft.server.packs.PackLocationInfo location, Pack.Metadata metadata) { return openPrimary(location); }
					},
					PackType.SERVER_DATA,
					new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, false));
				if (dragonballPack != null) packConsumer.accept(dragonballPack);
			});
		}
	}


    public static void registerAttributes() {
        // MAESTROS
        for (var masterEntity : MainEntities.getMasterEntities()) {
            FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) masterEntity.get(), MastersEntity.createAttributes().build());
        }

        // Quest NPC — single entity type for all data-driven quest NPCs | Usa un solo tipo de entidad para todos los NPCs de misiones basados en datos
        FabricDefaultAttributeRegistry.register(MainEntities.QUEST_NPC.get(), MastersEntity.createAttributes().build());

		for (var entity : MainEntities.getDragonWishEntities().values()) {
			FabricDefaultAttributeRegistry.register(entity.get(), DragonWishEntity.createAttributes().build());
		}

        var saibamans = List.of(
                MainEntities.SAGA_SAIBAMAN, MainEntities.SAGA_SAIBAMAN2, MainEntities.SAGA_SAIBAMAN3,
                MainEntities.SAGA_SAIBAMAN4, MainEntities.SAGA_SAIBAMAN5, MainEntities.SAGA_SAIBAMAN6
        );
        var soldiers = List.of(
                MainEntities.SAGA_FRIEZA_SOLDIER, MainEntities.SAGA_FRIEZA_SOLDIER2,
                MainEntities.SAGA_FRIEZA_SOLDIER3, MainEntities.SAGA_MORO_SOLDIER
        );
        var ozarus = List.of(
                MainEntities.SAGA_OZARU_VEGETA, MainEntities.SAGA_OZARU, MainEntities.SAGA_BABY_GOLDEN_OZARU
        );

        for (var saibaman : saibamans) FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) saibaman.get(), SagaSaibamanEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SAGA_BIOMAN.get(), SagaSaibamanEntity.BiomanEntity.createAttributes().build());
        for (var soldier : soldiers) FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) soldier.get(), SagaFriezaSoldier01Entity.createAttributes().build());
        for (var ozaru : ozarus) FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) ozaru.get(), SagaOzaruEntity.createAttributes().build());
        var ogres = List.of(MainEntities.SAGA_OGRE_RED, MainEntities.SAGA_OGRE_BLUE);
        for (var ogre : ogres) FabricDefaultAttributeRegistry.register(ogre.get(), SagaOgreEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.MINI_JANEMBA_STAMPEDE.get(), MiniJanembaStampedeEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SAGA_SAIYAN_SOLDIER.get(), DBSagasEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SAGA_SAIYAN_ELITE.get(), DBSagasEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SAGA_SAIYAN_OOZARU.get(), SagaSaiyanInvadersEntity.Oozaru.createAttributes().build());
        for (var clone : MainEntities.getEvilClones()) FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) clone.get(), EvilCloneEntity.createAttributes().build());


        AttributeSupplier defaultSagaAttributes = DBSagasEntity.createAttributes().build();

        for (var sagaEntity : MainEntities.getSagaEntities()) {
            if (saibamans.contains(sagaEntity) || soldiers.contains(sagaEntity) || ozarus.contains(sagaEntity) || ogres.contains(sagaEntity) || sagaEntity == MainEntities.MINI_JANEMBA_STAMPEDE || sagaEntity == MainEntities.WILD_GIRAN || sagaEntity == MainEntities.SAGA_BIOMAN) {
                continue;
            }
            FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) sagaEntity.get(), defaultSagaAttributes);
        }

        FabricDefaultAttributeRegistry.register(MainEntities.DINOSAUR1.get(), Dino1Entity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.DINOSAUR2.get(), Dino2Entity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.DINOSAUR3.get(), DinoFlyEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.DINO_KID.get(), DinoKidEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.NAMEK_FROG.get(), NamekFrogEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.NAMEK_FROG_GINYU.get(), NamekFrogGinyuEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.NAMEK_TRADER.get(), NamekTraderEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.CC_NAMEKIAN.get(), NamekTraderEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.NAMEK_WARRIOR.get(), NamekWarriorEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.DEMON_VILLAGER.get(), DemonVillagersEntity.Resident.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.TIME_PATROLLER.get(), DemonVillagersEntity.Resident.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SABERTOOTH.get(), SabertoothEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.WILD_GIRAN.get(), SagaZFightersEntity.WildGiranEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.GIANT_FISH.get(), GiantFishEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.GIANT_TURTLE.get(), GiantTurtleEntity.createAttributes());

        FabricDefaultAttributeRegistry.register(MainEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.RED_RIBBON_ROBOT1.get(), RobotEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.RED_RIBBON_ROBOT2.get(), RobotEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.RED_RIBBON_ROBOT3.get(), RobotEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.RED_RIBBON_SOLDIER.get(), RedRibbonSoldierEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.SPACE_POD.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.FLYING_NIMBUS.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.BLACK_NIMBUS.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.CAR.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.CC576.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.FRIEZA_CAPSULE.get(), VehicleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MainEntities.ROBOT_XENOVERSE.get(), RobotEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.PUNCH_MACHINE.get(), PunchMachineEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.MAJIN_SKILL.get(), MajinSkillEntity.createAttributes().build());
        FabricDefaultAttributeRegistry.register(MainEntities.BIO_CELL_JR.get(), DBSagasEntity.createAttributes().build());

    }

	public static void commonSetup() {
		new PredefinedTechniques().init();

			Regions.register(new OverworldRegion(40));
			SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Reference.MOD_ID, OverworldSurfaceRules.makeRules());
	}


    public static void registerPlayerAttributes() {
        FabricDefaultAttributeRegistry.MODIFY.register(context -> context.modify(EntityType.PLAYER, (type, attributes) -> {
        attributes.add( MainAttributes.STRENGTH);
        attributes.add( MainAttributes.STRIKE_POWER);
        attributes.add( MainAttributes.RESISTANCE);
        attributes.add( MainAttributes.VITALITY);
        attributes.add( MainAttributes.KI_POWER);
        attributes.add( MainAttributes.ENERGY);
        attributes.add( MainAttributes.MAX_ENERGY);
        attributes.add( MainAttributes.MAX_STAMINA);
        attributes.add( MainAttributes.MAX_POISE);
        attributes.add( MainAttributes.MELEE_DAMAGE);
        attributes.add( MainAttributes.STRIKE_DAMAGE);
        attributes.add( MainAttributes.DEFENSE);
		attributes.add( MainAttributes.CRIT_CHANCE);
		attributes.add( MainAttributes.CRIT_DAMAGE);
        }));
    }

    public static void registerMachineStorage() {
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be.getItemHandler(), side),
                MainBlockEntities.KIKONO_STATION_BE.get());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be.getItemHandler(), side),
                MainBlockEntities.FUEL_GENERATOR_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.KIKONO_STATION_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.FUEL_GENERATOR_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.ENERGY_CABLE_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.GRAVITY_DEVICE_BE.get());
    }

}
