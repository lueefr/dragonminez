package com.dragonminez.platform.event.tick;
import com.dragonminez.platform.event.Event;
public abstract class ClientTickEvent extends Event {
    public static final class Pre extends ClientTickEvent {}
    public static final class Post extends ClientTickEvent {}
}
