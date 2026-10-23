package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.InputEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Minecraft.class)
public abstract class InteractionInputBridgeMixin {
    private InputEvent.InteractionKeyMappingTriggered dmz$interaction(int button) {
        return GameEvents.BUS.post(new InputEvent.InteractionKeyMappingTriggered(button, InteractionHand.MAIN_HAND));
    }
    private void dmz$swing(InputEvent.InteractionKeyMappingTriggered event) {
        var player = ((Minecraft) (Object) this).player;
        if (player != null && event.shouldSwingHand()) player.swing(event.getHand());
    }
    @WrapMethod(method = "startAttack")
    private boolean dmz$attack(Operation<Boolean> original) {
        var event = dmz$interaction(0);
        if (!event.isCanceled()) return original.call();
        dmz$swing(event); return false;
    }
    @WrapMethod(method = "continueAttack")
    private void dmz$continueAttack(boolean held, Operation<Void> original) {
        if (!held) { original.call(false); return; }
        var event = dmz$interaction(0);
        if (!event.isCanceled()) original.call(true);
        else { original.call(false); dmz$swing(event); }
    }
    @WrapMethod(method = "startUseItem")
    private void dmz$use(Operation<Void> original) {
        var event = dmz$interaction(1);
        if (!event.isCanceled()) original.call(); else dmz$swing(event);
    }
    @WrapMethod(method = "pickBlockOrEntity")
    private void dmz$pick(Operation<Void> original) {
        var event = dmz$interaction(2);
        if (!event.isCanceled()) original.call();
    }
}
