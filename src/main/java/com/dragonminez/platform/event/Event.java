package com.dragonminez.platform.event;

/** Mutable, synchronous DragonMineZ event. Fabric callbacks and mixins provide the native hooks. */
public abstract class Event {
    public enum Result { DENY, DEFAULT, ALLOW }
    private boolean canceled;
    public final boolean isCanceled() { return canceled; }
    public final void setCanceled(boolean value) {
        if (!(this instanceof ICancellableEvent)) throw new IllegalStateException("Event is not cancellable: " + getClass().getName());
        canceled=value;
    }
}
