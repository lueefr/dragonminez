package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.Event;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.InteractionHand;
public abstract class InputEvent extends Event {
    public static final class Key extends InputEvent {
        private final net.minecraft.client.input.KeyEvent key;
        private final int action;
        public Key(net.minecraft.client.input.KeyEvent key, int action) { this.key = key; this.action = action; }
        public int getKey() { return key.key(); }
        public int getScanCode() { return key.scancode(); }
        public int getModifiers() { return key.modifiers(); }
        public int getAction() { return action; }
    }
    public static final class MouseScrollingEvent extends InputEvent implements ICancellableEvent {
        private final double x, y;
        public MouseScrollingEvent(double x, double y) { this.x = x; this.y = y; }
        public double getScrollDeltaX() { return x; }
        public double getScrollDeltaY() { return y; }
    }
    public static final class InteractionKeyMappingTriggered extends InputEvent implements ICancellableEvent {
        private final int button;
        private final InteractionHand hand;
        private boolean swing = true;
        public InteractionKeyMappingTriggered(int button, InteractionHand hand) { this.button = button; this.hand = hand; }
        public boolean isAttack() { return button == 0; }
        public boolean isUseItem() { return button == 1; }
        public boolean isPickBlock() { return button == 2; }
        public int getButton() { return button; }
        public InteractionHand getHand() { return hand; }
        public boolean shouldSwingHand() { return swing; }
        public void setSwingHand(boolean value) { swing = value; }
    }
}
