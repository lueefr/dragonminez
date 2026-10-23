package com.dragonminez.platform.event;

import com.dragonminez.common.events.DMZEvent;

/** Explicit subscriptions for the loader-independent DragonMineZ gameplay API. */
public final class ClientCustomEventSubscribers {
    private static boolean installed;
    private ClientCustomEventSubscribers() {}
    public static synchronized void register() {
        if (installed) return;

        installed = true;
    }
}
