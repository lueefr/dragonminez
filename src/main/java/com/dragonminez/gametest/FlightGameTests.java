package com.dragonminez.gametest;

import com.dragonminez.api.flight.FlightMotion;
import com.dragonminez.api.flight.FlightMotion.*;
import com.dragonminez.common.flight.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class FlightGameTests {
    @GameTest(template = "port_empty")
    public static void physicalFlight(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PhysicalFlight"));
        var connection = player.connection;
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        var origin = helper.absoluteVec(new Vec3(1.5, 3, 1.5));
        var wall = BlockPos.containing(origin.add(0, 0, 3));
        try {
            data.getStatus().setHasCreatedCharacter(true);
            data.getSkills().registerDefaultSkill("fly", 10);
            data.getSkills().setSkillLevel("fly", 1); data.getSkills().setSkillActive("fly", true);
            player.setPos(origin); player.setYRot(0); player.setXRot(0);
            field.set(null, RuntimeConfig.parse("{\"flight\":{\"enabled\":true}}"));
            var state = FlightMotion.initial(FlightBody.vector(origin), Vector.ZERO, 0, 0);
            var shape = FlightBody.shape(player, state);
            helper.assertTrue(FlightBody.bounds(shape).getZsize() > FlightBody.bounds(shape).getYsize(), "Flight body remained upright");
            helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
            var after = new State(state.position().add(new Vector(0, 0, 5)), new Vector(0, 0, 5), state.rotation(), Vector.ZERO);
            var resolved = FlightBody.move(player, state, after);
            helper.assertTrue(resolved.position().z() < FlightBody.vector(origin).z() + 3 && resolved.velocity().equals(Vector.ZERO), "Swept flight crossed a solid wall");
            helper.assertTrue(FlightBody.clear(player, resolved), "Collision stopped inside wall");
            var high = new State(new Vector(state.position().x(), helper.getLevel().getMaxBuildHeight() + 20, state.position().z()),
                    Vector.ZERO, state.rotation(), Vector.ZERO);
            helper.assertTrue(FlightBody.clear(player, high), "Flight incorrectly treated build height as a solid ceiling");
            var rolled = new State(state.position(), Vector.ZERO,
                    state.rotation().multiply(FlightMotion.axis(0, 0, 1, Math.PI / 4)), Vector.ZERO);
            var turned = FlightBody.move(player, state, rolled);
            helper.assertTrue(FlightBody.vector(FlightBody.shape(player, turned).center()).distanceTo(FlightBody.vector(shape.center())) < 1e-8,
                    "Rolling shifted the body center instead of rotating around it");
            FlightServer.advance(player);
            helper.assertTrue(FlightFrames.active(player), "Server did not own eligible flight");
            Vec3 before = player.position();
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(before.x + 40, before.y, before.z, false));
            helper.assertTrue(player.position().equals(before) && FlightFrames.active(player), "Client position packet overrode flight authority or triggered native correction");
            player.travel(new Vec3(1, 0, 1));
            helper.assertTrue(player.position().equals(before), "Native travel moved authoritative body twice");
            helper.assertTrue(player.getEyePosition().y < before.y + FlightBody.dimensions(player).eyeHeight(), "Eye origin ignored prone orientation");
            helper.assertTrue(Math.abs(player.getEyeY() - player.getEyePosition().y) < 1e-6
                    && Math.abs(player.getEyeHeight() + player.getY() - player.getEyeY()) < 1e-6, "Native eye queries retained standing height");
            helper.assertTrue(Math.abs(player.getBbHeight() - player.getBoundingBox().getYsize()) < 1e-6
                    && Math.abs(player.getBbWidth() - player.getBoundingBox().getXsize()) < 1e-6, "Native size queries ignored flight body");
            com.dragonminez.server.events.players.combat.KnockbackHelper.apply(player, new Vec3(.4, .2, 0));
            helper.assertTrue(FlightFrames.get(player).velocity().equals(new Vector(.4, .2, 0)), "Flight authority erased combat knockback");
            player.knockback(.4, 1, 0);
            helper.assertTrue(FlightFrames.get(player).velocity().equals(FlightBody.vector(player.getDeltaMovement())), "Flight authority erased native knockback");
            var angled = FlightMotion.initial(FlightBody.vector(origin), Vector.ZERO, 45, 0);
            FlightFrames.put(player, angled); FlightBody.apply(player, angled);
            var miss = origin.add(.7, .25, .7);
            var start = miss.add(0, -1, 0); var end = miss.add(0, 1, 0);
            helper.assertTrue(player.getBoundingBox().clip(start, end).isPresent(), "Test ray missed broad phase");
            helper.assertTrue(!com.dragonminez.common.combat.util.MultipartTargeting.intersects(player, start, end, 0), "Ki targeting hit an empty corner of rotated body");
            helper.assertTrue(com.dragonminez.common.combat.util.MultipartTargeting.intersects(player,
                    origin.add(0, -1, 0), origin.add(0, 1, 0), 0), "Ki targeting missed actual flight body");
            player.connection.teleport(origin.x, origin.y, origin.z, 0, 0);
            helper.assertTrue(!FlightFrames.active(player) && ((FlightConnection) player.connection).awaiting(), "Teleport retained physical pose or lost acknowledgement state");
            helper.succeed();
        } finally {
            FlightServer.stop(player); field.set(null, original);
            helper.getLevel().setBlock(wall, Blocks.AIR.defaultBlockState(), 3); player.connection = connection; player.discard();
        }
    }
}
