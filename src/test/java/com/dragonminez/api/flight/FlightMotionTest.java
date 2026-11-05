package com.dragonminez.api.flight;

import org.junit.jupiter.api.Test;
import static com.dragonminez.api.flight.FlightMotion.*;
import static org.junit.jupiter.api.Assertions.*;

class FlightMotionTest {
    private final Tuning tuning = new Tuning(.08, .04, .3, 0, 0, 1, 2, .15, .35, .2);

    @Test void deterministicReplay() {
        var a = initial(Vector.ZERO, Vector.ZERO, 35, -20);
        var b = a;
        for (int i = 0; i < 2000; i++) {
            var input = new Controls(Math.sin(i), Math.cos(i), .5, 1, 0, true, false);
            a = step(a, input, tuning);
            b = step(b, input, tuning);
            assertEquals(a, b);
            assertTrue(a.velocity().length() <= 2.000000001);
            var x = a.rotation().rotate(new Vector(1, 0, 0));
            var y = a.rotation().rotate(new Vector(0, 1, 0));
            assertEquals(1, x.length(), 1e-10);
            assertEquals(0, x.dot(y), 1e-10);
        }
    }

    @Test void completeLoops() {
        var state = initial(Vector.ZERO, Vector.ZERO, 0, 0);
        var direct = new Tuning(0, 0, 0, 0, 0, 1, 1, Math.PI / 8, 1, 0);
        for (int i = 0; i < 16; i++) state = step(state, new Controls(1, 0, 0, 0, 0, false, false), direct);
        var forward = state.rotation().rotate(new Vector(0, 0, -1));
        assertEquals(0, forward.y(), 1e-10);
        assertEquals(1, forward.z(), 1e-10);
    }

    @Test void brakingLimits() {
        var state = initial(Vector.ZERO, new Vector(0, 0, 1), 0, 0);
        assertTrue(step(state, new Controls(0, 0, 0, 0, 0, false, true), tuning).velocity().length()
                < step(state, Controls.ZERO, tuning).velocity().length());
        assertThrows(IllegalArgumentException.class, () -> new Controls(2, 0, 0, 0, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Vector(Double.NaN, 0, 0));
    }

    @Test void yawDirection() {
        var state = initial(Vector.ZERO, Vector.ZERO, 0, 0);
        var direct = new Tuning(0, 0, 0, 0, 0, 1, 1, .15, 1, 0);
        var right = step(state, new Controls(0, 1, 0, 0, 0, false, false), direct);
        var left = step(state, new Controls(0, -1, 0, 0, 0, false, false), direct);
        assertTrue(yaw(right.rotation()) > 0);
        assertTrue(yaw(left.rotation()) < 0);
        assertTrue(right.rotation().rotate(new Vector(0, 0, -1)).x() < 0);
        assertTrue(left.rotation().rotate(new Vector(0, 0, -1)).x() > 0);
    }

    @Test void orientedHits() {
        var shape = new FlightShape(Vector.ZERO, axis(0, 1, 0, Math.PI / 4), new Vector(.3, .3, 1));
        assertTrue(shape.radius().x() > .9);
        assertFalse(shape.contains(new Vector(.8, 0, -.8)));
        assertTrue(shape.clip(new Vector(.8, -1, -.8), new Vector(.8, 1, -.8), 0).isEmpty());
        assertTrue(shape.clip(new Vector(0, -1, 0), new Vector(0, 1, 0), 0).isPresent());
        assertEquals(0, shape.clip(Vector.ZERO, new Vector(4, 0, 0), 0).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> shape.clip(Vector.ZERO, Vector.ZERO, -1));
    }

    @Test void sessionInputs() {
        var input = new FlightInput(42);
        var controls = new Controls(0, 0, 1, 1, 0, true, false);
        assertFalse(input.accept(41, 1, 10, controls));
        assertTrue(input.accept(42, 1, 10, controls));
        assertFalse(input.accept(42, 2, 10, controls));
        assertFalse(input.accept(42, 1, 11, controls));
        assertFalse(input.accept(42, 130, 11, controls));
        assertEquals(controls, input.controls(20));
        assertTrue(input.controls(21).brake());
        assertEquals(0, input.controls(21).throttle());
        assertTrue(input.accept(42, 2, 22, controls));
        assertEquals(2, input.sequence());
    }

    @Test void axisRates() {
        var state = initial(Vector.ZERO, Vector.ZERO, 0, 0);
        var independent = new Tuning(0, 0, 0, 0, 0, 1, 1, .15, 1, 0, 0, 0, .3);
        var rolled = step(state, new Controls(1, 1, 1, 0, 0, false, false), independent);
        assertEquals(0, yaw(rolled.rotation()), 1e-8);
        assertEquals(0, pitch(rolled.rotation()), 1e-8);
        assertTrue(roll(rolled.rotation()) > 15);
        assertEquals(tuning.turn(), tuning.pitchRate());
        assertEquals(tuning.turn(), tuning.yawRate());
        assertEquals(tuning.turn(), tuning.rollRate());
        assertThrows(IllegalArgumentException.class, () -> new Tuning(0, 0, 0, 0, 0, 1, 1, .15, 1, 0, 0, 0, 1));
    }
}
