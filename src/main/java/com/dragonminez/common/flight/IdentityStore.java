package com.dragonminez.common.flight;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Entity equality compares IDs; transient side-local state must compare object identity instead. */
final class IdentityStore<K, V> {
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        Key(K value, ReferenceQueue<K> queue) {
            super(java.util.Objects.requireNonNull(value), queue);
            hash = System.identityHashCode(value);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key<?> key && get() != null && get() == key.get();
        }
    }
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();
    private final Map<Key<K>, V> values = new HashMap<>();

    private void drain() {
        java.lang.ref.Reference<? extends K> key;
        while ((key = queue.poll()) != null) values.remove(key);
    }
    public synchronized V get(K key) {
        drain();
        return key == null ? null : values.get(new Key<>(key, null));
    }
    public synchronized void put(K key, V value) {
        drain();
        values.put(new Key<>(key, queue), java.util.Objects.requireNonNull(value));
    }
    public synchronized V remove(K key) {
        drain();
        return key == null ? null : values.remove(new Key<>(key, null));
    }
    public synchronized boolean any(Predicate<K> filter) {
        drain();
        for (var key : values.keySet()) {
            var value = key.get();
            if (value != null && filter.test(value)) return true;
        }
        return false;
    }
}
