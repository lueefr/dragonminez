package com.dragonminez.platform.event;

public interface ICancellableEvent {
    boolean isCanceled();
    void setCanceled(boolean canceled);
}
