package com.dragonminez.client.flight;

import com.dragonminez.api.flight.FlightBlend;
import com.dragonminez.api.flight.FlightMotion.State;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.flight.*;
import com.dragonminez.common.network.S2C.FlightStateS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class FlightVisuals {
    private record Frame(long session,long sequence,boolean active,FlightBlend blend){}
    private static final IdentityStore<Player,Frame> FRAMES=new IdentityStore<>();
    private FlightVisuals(){}
    public static boolean receive(Player player,FlightStateS2C message){
        var previous=FRAMES.get(player);
        if(previous!=null&&(message.session()<previous.session()||message.session()==previous.session()&&(message.sequence()<previous.sequence()||!previous.active()&&message.active())))return false;
        var blend=previous==null||previous.session()!=message.session()?new FlightBlend(message.state()):previous.blend();
        blend.update(message.state(),System.nanoTime(),ConfigManager.getUserConfig().flightBlend());
        FRAMES.put(player,new Frame(message.session(),message.sequence(),message.active(),blend));return true;
    }
    public static net.minecraft.world.phys.Vec3 position(Player player,float partial){
        var state=pose(player);
        if(state!=null&&player!=Minecraft.getInstance().player)return FlightBody.vector(state.position());
        return new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.lerp(partial,player.xo,player.getX()),net.minecraft.util.Mth.lerp(partial,player.yo,player.getY()),net.minecraft.util.Mth.lerp(partial,player.zo,player.getZ()));
    }
    public static State pose(Player player){
        var state=FlightFrames.get(player);
        if(state==null||player==Minecraft.getInstance().player)return state;
        var frame=FRAMES.get(player);return frame==null||!frame.active()?state:frame.blend().sample(System.nanoTime());
    }
}
