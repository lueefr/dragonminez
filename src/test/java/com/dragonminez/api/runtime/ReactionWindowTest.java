package com.dragonminez.api.runtime;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReactionWindowTest {
    @Test void independentProjectiles() {
        var windows = new ReactionWindow(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        assertTrue(windows.attempt(first, "example:one", 100, 10));
        assertFalse(windows.attempt(first, "example:one", 109, 10));
        assertTrue(windows.attempt(second, "example:one", 109, 10));
        assertTrue(windows.attempt(first, "example:other", 109, 10));
        assertTrue(windows.attempt(first, "example:one", 110, 10));
    }
    @Test void resetAndDefaults() {
        var windows = new ReactionWindow(); var id = UUID.randomUUID();
        assertTrue(windows.attempt(id, "example:one", 100, 0)); assertTrue(windows.attempt(id, "example:one", 100, 0));
        assertTrue(windows.attempt(id, "example:one", 100, 20));
        assertTrue(windows.attempt(id, "example:one", 10, 20));
        windows.clear(); assertTrue(windows.attempt(id, "example:one", 11, 20));
        assertThrows(IllegalArgumentException.class, () -> windows.attempt(id, "example:one", 11, -1));
    }
}
