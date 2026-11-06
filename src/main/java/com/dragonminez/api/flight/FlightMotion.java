package com.dragonminez.api.flight;

/** Fixed-tick flight math shared by authority and prediction; no game or client dependencies. */
public final class FlightMotion {
    private FlightMotion() {}
    public record Vector(double x, double y, double z) {
        public static final Vector ZERO = new Vector(0, 0, 0);
        public Vector { finite(x); finite(y); finite(z); }
        public Vector add(Vector v) { return new Vector(x + v.x, y + v.y, z + v.z); }
        public Vector scale(double n) { return new Vector(x * n, y * n, z * n); }
        public double length() { return Math.hypot(Math.hypot(x, y), z); }
        public double dot(Vector v) { return x * v.x + y * v.y + z * v.z; }
        public Vector cross(Vector v) { return new Vector(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
    }
    public record Rotation(double x, double y, double z, double w) {
        public Rotation { finite(x); finite(y); finite(z); finite(w); }
        public Rotation unit() {
            double length = Math.hypot(Math.hypot(x,y),Math.hypot(z,w));
            if (length < 1e-12) throw new IllegalArgumentException("Flight rotation cannot be zero");
            return new Rotation(x/length,y/length,z/length,w/length);
        }
        public Rotation multiply(Rotation b) {
            return new Rotation(w*b.x+x*b.w+y*b.z-z*b.y, w*b.y-x*b.z+y*b.w+z*b.x,
                    w*b.z+x*b.y-y*b.x+z*b.w, w*b.w-x*b.x-y*b.y-z*b.z);
        }
        public Vector rotate(Vector v) {
            var q = new Vector(x,y,z); var cross = q.cross(v).scale(2);
            return v.add(cross.scale(w)).add(q.cross(cross));
        }
        public Rotation inverse() { return new Rotation(-x,-y,-z,w); }
        public Rotation blend(Rotation b, double amount) {
            double sign = x*b.x+y*b.y+z*b.z+w*b.w < 0 ? -1 : 1;
            return new Rotation(x*(1-amount)+b.x*sign*amount,y*(1-amount)+b.y*sign*amount,z*(1-amount)+b.z*sign*amount,w*(1-amount)+b.w*sign*amount).unit();
        }
    }
    public record Controls(double pitch, double yaw, double roll, double throttle, double lift, boolean boost, boolean brake) {
        public static final Controls ZERO = new Controls(0,0,0,0,0,false,false);
        public Controls {
            for (double value : new double[]{pitch,yaw,roll,throttle,lift}) if (!Double.isFinite(value) || Math.abs(value)>1) throw new IllegalArgumentException("Flight input outside -1..1");
        }
    }
    public record Tuning(double thrust, double drag, double braking, double gravity, double lift, double speed, double boost, double turn, double smoothing, double banking) {
        public Tuning {
            for (double n : new double[]{thrust,drag,braking,gravity,lift,speed,boost,turn,smoothing,banking}) if (!Double.isFinite(n) || n<0) throw new IllegalArgumentException("Flight tuning must be finite and nonnegative");
            if (speed<=0 || speed>8 || boost<1 || boost>4 || drag>1 || braking>1 || turn>Math.PI/4 || smoothing>1 || thrust>1 || gravity>1 || lift>1 || banking>1) throw new IllegalArgumentException("Flight tuning outside bounds");
        }
    }
    public record State(Vector position, Vector velocity, Rotation rotation, Vector angular) {
        public State { java.util.Objects.requireNonNull(position); java.util.Objects.requireNonNull(velocity); rotation=rotation.unit(); java.util.Objects.requireNonNull(angular); }
    }
    public static State initial(Vector position, Vector velocity, double yaw, double pitch) {
        return new State(position,velocity,axis(0,1,0,Math.PI-Math.toRadians(yaw)).multiply(axis(1,0,0,-Math.toRadians(pitch))).unit(),Vector.ZERO);
    }
    public static State step(State state, Controls controls, Tuning tuning) {
        var target = new Vector(-controls.pitch(),-controls.yaw(),-controls.roll()-controls.yaw()*tuning.banking());
        var angular = state.angular().scale(1-tuning.smoothing()).add(target.scale(tuning.smoothing()));
        var rotation = state.rotation().multiply(axis(0,1,0,angular.y()*tuning.turn())).multiply(axis(1,0,0,angular.x()*tuning.turn())).multiply(axis(0,0,1,angular.z()*tuning.turn())).unit();
        var forward = rotation.rotate(new Vector(0,0,-1)); var up = rotation.rotate(new Vector(0,1,0));
        double ratio = Math.min(1,state.velocity().length()/tuning.speed());
        var force = forward.scale(controls.throttle()*tuning.thrust()*(controls.boost()?tuning.boost():1))
                .add(up.scale(controls.lift()*tuning.thrust()+tuning.lift()*ratio*ratio)).add(new Vector(0,-tuning.gravity(),0));
        double drag = controls.brake()?tuning.braking():tuning.drag();
        var velocity = state.velocity().scale(1-drag).add(force);
        double limit = tuning.speed()*(controls.boost()?tuning.boost():1), length = velocity.length();
        if (length>limit) velocity=velocity.scale(limit/length);
        return new State(state.position().add(velocity),velocity,rotation,angular);
    }
    public static Rotation axis(double x,double y,double z,double angle) {
        finite(angle); double half=angle/2,sin=Math.sin(half); return new Rotation(x*sin,y*sin,z*sin,Math.cos(half));
    }
    public static double yaw(Rotation rotation) { var v=rotation.rotate(new Vector(0,0,-1)); return Math.toDegrees(Math.atan2(-v.x(),v.z())); }
    public static double pitch(Rotation rotation) { return Math.toDegrees(-Math.asin(Math.clamp(rotation.rotate(new Vector(0,0,-1)).y(),-1,1))); }
    public static double roll(Rotation rotation) {
        var upright=initial(Vector.ZERO,Vector.ZERO,yaw(rotation),pitch(rotation)).rotation();
        var relative=upright.inverse().multiply(rotation); return -Math.toDegrees(2*Math.atan2(relative.z(),relative.w()));
    }
    private static void finite(double n) { if (!Double.isFinite(n)) throw new IllegalArgumentException("Flight state must be finite"); }
}
