package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.CarEntity;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class SpacePodGameTests {

	@GameTest(template = "port_empty")
	public static void namekDestination(GameTestHelper helper) {
		var namek = SpacePodDestinationRegistry.getServerDestination("namek");
		helper.assertTrue(namek != null && "dragonminez:namek".equals(namek.dimension()),
				"Space Pod did not resolve the Namek destination by ID");
		helper.succeed();
	}

	@GameTest(template = "port_empty")
	public static void seatPositions(GameTestHelper helper) {
		var level = helper.getLevel();
		CarEntity car = CarEntity.create(level, CarEntity.Color.RED);
		BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
		car.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		level.addFreshEntity(car);

		Pig first = EntityType.PIG.create(level);
		Pig second = EntityType.PIG.create(level);
		try {
			helper.assertTrue(first != null && second != null, "Could not create test passengers");
			first.setPos(car.getX(), car.getY(), car.getZ());
			second.setPos(car.getX(), car.getY(), car.getZ());
			level.addFreshEntity(first);
			level.addFreshEntity(second);
			helper.assertTrue(first.startRiding(car, true), "First passenger could not mount");
			helper.assertTrue(second.startRiding(car, true), "Second passenger could not mount");
			helper.assertTrue(car.getSeatIndex(first) == 0 && car.getSeatIndex(second) == 1,
				"Rider seat indices are incorrect");

			Vec3 firstPos = car.getPassengerRidingPosition(first);
			Vec3 secondPos = car.getPassengerRidingPosition(second);
			helper.assertTrue(firstPos.distanceToSqr(secondPos) > 0.01D,
				"Both riders have the same attachment point");
			car.positionRider(first);
			car.positionRider(second);
			helper.assertTrue(first.position().distanceToSqr(firstPos) < 1.0E-5D
					&& second.position().distanceToSqr(secondPos) < 1.0E-5D,
				"Passenger positions do not match the engine attachment points");
			helper.succeed();
		} finally {
			first.discard();
			second.discard();
			car.discard();
		}
	}

	@GameTest(template = "port_empty")
	public static void podSeat(GameTestHelper helper) {
		SpacePodEntity pod = new SpacePodEntity(MainEntities.SPACE_POD.get(), helper.getLevel());
		var seat = pod.getProperties().seats().getFirst();
		double hip = pod.getProperties().modelToLocal(0.0D, 10.9D, -1.0D).y;
		helper.assertTrue(Math.abs(seat.y - (hip - 0.6D - 0.35D)) < 1.0E-5D,
				"Space Pod hip attachment does not compensate the rider offset");
		helper.succeed();
	}
}
