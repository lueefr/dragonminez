package com.dragonminez.common.flight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityStoreTest {
    private record Entity(int id) {}

    @Test void sideIsolation() {
        var server = new Entity(42);
        var client = new Entity(42);
        assertEquals(server, client);
        var store = new IdentityStore<Entity, String>();
        store.put(server, "authority"); store.put(client, "prediction");
        assertEquals("authority", store.get(server));
        assertEquals("prediction", store.get(client));
        store.put(client, "replayed");
        assertEquals("authority", store.get(server));
        assertEquals("replayed", store.remove(client));
        assertEquals("authority", store.get(server));
        assertNull(store.get(client));
        assertTrue(store.any(entity -> entity == server));
        assertFalse(store.any(entity -> entity == client));
    }
}
