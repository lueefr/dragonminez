package com.dragonminez.api.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackStateTest {
    @Test void timedPhases() {
        var attack = new AttackState(); attack.start("example:beam");
        assertFalse(attack.tick(true, "example:beam", 75));
        assertEquals(75d, attack.numbers().get("attack.charge"));
        attack.release("example:beam", 150, 2, 3);
        assertFalse(attack.tick(false, "", 0)); assertTrue(attack.tick(false, "", 0));
        assertEquals(AttackState.Phase.RECOVERY, attack.phase());
        assertFalse(attack.tick(false, "", 0)); assertFalse(attack.tick(false, "", 0)); assertTrue(attack.tick(false, "", 0));
        assertEquals(AttackState.Phase.IDLE, attack.phase()); assertEquals("", attack.technique());
    }
    @Test void interruptedCharge() {
        var attack = new AttackState(); attack.start("example:beam");
        assertTrue(attack.tick(true, "example:other", 5));
        assertEquals(AttackState.Phase.IDLE, attack.phase());
        attack.start("example:beam"); assertTrue(attack.tick(false, "", 0));
    }
    @Test void synchronizedQueries() {
        var attack = new AttackState(); attack.release("example:beam", 80, 2, 0);
        var client = new AttackState(); client.view(attack.numbers());
        assertEquals(attack.numbers(), client.numbers());
        assertThrows(IllegalArgumentException.class, () -> attack.release("example:beam", Double.NaN, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> attack.release("example:beam", 50, 0, 0));
    }
}
