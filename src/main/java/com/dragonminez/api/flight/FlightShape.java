package com.dragonminez.api.flight;

import com.dragonminez.api.flight.FlightMotion.Rotation;
import com.dragonminez.api.flight.FlightMotion.Vector;
import java.util.OptionalDouble;

/** Body geometry shared by server collision and client prediction. */
public record FlightShape(Vector center, Rotation rotation, Vector extent) {
    public FlightShape {
        java.util.Objects.requireNonNull(center);
        rotation = rotation.unit();
        if (extent.x() <= 0 || extent.y() <= 0 || extent.z() <= 0) {
            throw new IllegalArgumentException("Flight body dimensions must be positive");
        }
    }

    public Vector radius() {
        var x = rotation.rotate(new Vector(extent.x(), 0, 0));
        var y = rotation.rotate(new Vector(0, extent.y(), 0));
        var z = rotation.rotate(new Vector(0, 0, extent.z()));
        return new Vector(Math.abs(x.x()) + Math.abs(y.x()) + Math.abs(z.x()),
                Math.abs(x.y()) + Math.abs(y.y()) + Math.abs(z.y()),
                Math.abs(x.z()) + Math.abs(y.z()) + Math.abs(z.z()));
    }

    public boolean contains(Vector point) {
        var local = rotation.inverse().rotate(point.add(center.scale(-1)));
        return Math.abs(local.x()) <= extent.x() && Math.abs(local.y()) <= extent.y()
                && Math.abs(local.z()) <= extent.z();
    }

    /** Returns the first segment fraction, including zero when starting inside. */
    public OptionalDouble clip(Vector start, Vector end, double padding) {
        if (!Double.isFinite(padding) || padding < 0) {
            throw new IllegalArgumentException("Flight collision padding must be nonnegative");
        }
        var inverse = rotation.inverse();
        var local = inverse.rotate(start.add(center.scale(-1)));
        var delta = inverse.rotate(end.add(start.scale(-1)));
        double near = 0, far = 1;
        double[] origins = {local.x(), local.y(), local.z()};
        double[] directions = {delta.x(), delta.y(), delta.z()};
        double[] bounds = {extent.x() + padding, extent.y() + padding, extent.z() + padding};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(directions[i]) < 1e-12) {
                if (Math.abs(origins[i]) > bounds[i]) return OptionalDouble.empty();
            } else {
                double a = (-bounds[i] - origins[i]) / directions[i];
                double b = (bounds[i] - origins[i]) / directions[i];
                near = Math.max(near, Math.min(a, b));
                far = Math.min(far, Math.max(a, b));
                if (near > far) return OptionalDouble.empty();
            }
        }
        return OptionalDouble.of(near);
    }
}
