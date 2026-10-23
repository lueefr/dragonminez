package com.dragonminez.platform.event;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Ordered dispatch preserves cancellation and priority semantics of the original gameplay handlers. */
public final class EventBus {
    private record Listener(Class<? extends Event> type, EventPriority priority, boolean receiveCanceled,
                            long sequence, Consumer<Event> callback) {}
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Set<Object> registered = Collections.newSetFromMap(new IdentityHashMap<>());
    private final AtomicLong sequence = new AtomicLong();

    public <E extends Event> void addListener(Class<E> type, EventPriority priority, boolean receiveCanceled, Consumer<E> callback) {
        Objects.requireNonNull(callback);
        listeners.add(new Listener(type,priority,receiveCanceled,sequence.getAndIncrement(),event -> callback.accept(type.cast(event))));
        listeners.sort(Comparator.comparing(Listener::priority).thenComparingLong(Listener::sequence));
    }
    public synchronized void register(Object handler) {
        if (!registered.add(handler)) return;
        Class<?> type = handler instanceof Class<?> clazz ? clazz : handler.getClass();
        Method[] methods=type.getDeclaredMethods();
        Arrays.sort(methods,Comparator.comparing(Method::toGenericString));
        for (Method method : methods) {
            SubscribeEvent annotation=method.getAnnotation(SubscribeEvent.class);
            if (annotation==null) continue;
            if (method.getParameterCount()!=1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]) || method.getReturnType()!=void.class) {
                throw new IllegalArgumentException("Invalid event handler: " + method);
            }
            if (handler instanceof Class<?> && !Modifier.isStatic(method.getModifiers())) throw new IllegalArgumentException("Expected static event handler: " + method);
            method.setAccessible(true);
            Class<? extends Event> eventType=method.getParameterTypes()[0].asSubclass(Event.class);
            addListener(eventType,annotation.priority(),annotation.receiveCanceled(),event -> {
                try { method.invoke(Modifier.isStatic(method.getModifiers()) ? null : handler,event); }
                catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof RuntimeException cause) throw cause;
                    if (exception.getCause() instanceof Error cause) throw cause;
                    throw new IllegalStateException("Event handler failed: " + method,exception.getCause());
                } catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot invoke event handler: " + method,exception); }
            });
        }
    }
    public <E extends Event> E post(E event) {
        Objects.requireNonNull(event);
        for (Listener listener : listeners) {
            if (listener.type.isInstance(event) && (!event.isCanceled() || listener.receiveCanceled)) listener.callback.accept(event);
        }
        return event;
    }
}
