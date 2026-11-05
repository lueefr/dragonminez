package com.dragonminez.api.flight;

/** A session accepts controls, never a client position or client simulation clock. */
public final class FlightInput {
    private final long session;
    private long sequence;
    private long received = Long.MIN_VALUE;
    private FlightMotion.Controls controls = FlightMotion.Controls.ZERO;

    public FlightInput(long session) { this.session = session; }

    public boolean accept(long session, long sequence, long tick, FlightMotion.Controls controls) {
        if (session != this.session || sequence <= this.sequence || sequence - this.sequence > 128
                || tick <= received) return false;
        java.util.Objects.requireNonNull(controls);
        this.sequence = sequence;
        received = tick;
        this.controls = controls;
        return true;
    }

    public long sequence() { return sequence; }

    public FlightMotion.Controls controls(long tick) {
        return received == Long.MIN_VALUE || tick - received > 10
                ? new FlightMotion.Controls(0, 0, 0, 0, 0, false, true) : controls;
    }
}
