package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.Event;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
public abstract class ViewportEvent extends Event {
    private final Camera camera;
    private final double partialTick;
    private ViewportEvent(Camera camera, double partialTick) { this.camera = camera; this.partialTick = partialTick; }
    public Camera getCamera() { return camera; }
    public double getPartialTick() { return partialTick; }
    public static final class ComputeFov extends ViewportEvent {
        private double fov;
        public ComputeFov(Camera camera, double partialTick, double fov) { super(camera, partialTick); this.fov = fov; }
        public double getFOV() { return fov; }
        public void setFOV(double fov) { this.fov = fov; }
    }
    public static final class ComputeCameraAngles extends ViewportEvent {
        private float yaw, pitch, roll;
        public ComputeCameraAngles(Camera camera, double partialTick, float yaw, float pitch) {
            super(camera, partialTick); this.yaw = yaw; this.pitch = pitch;
        }
        public float getYaw() { return yaw; }
        public void setYaw(float value) { yaw = value; }
        public float getPitch() { return pitch; }
        public void setPitch(float value) { pitch = value; }
        public float getRoll() { return roll; }
        public void setRoll(float value) { roll = value; }
    }
    public static final class ComputeFogColor extends ViewportEvent {
        private float red, green, blue;
        public ComputeFogColor(Camera camera, double partialTick, float red, float green, float blue) {
            super(camera, partialTick); this.red = red; this.green = green; this.blue = blue;
        }
        public float getRed() { return red; }
        public void setRed(float value) { red = value; }
        public float getGreen() { return green; }
        public void setGreen(float value) { green = value; }
        public float getBlue() { return blue; }
        public void setBlue(float value) { blue = value; }
    }
    public static final class RenderFog extends ViewportEvent implements ICancellableEvent {
        private final FogType type;
        private float near, far;
        public RenderFog(Camera camera, double partialTick, FogType type, float near, float far) {
            super(camera, partialTick); this.type = type; this.near = near; this.far = far;
        }
        public FogRenderer.FogMode getMode() { return FogRenderer.FogMode.WORLD; }
        public FogType getType() { return type; }
        public float getNearPlaneDistance() { return near; }
        public void setNearPlaneDistance(float value) { near = value; }
        public float getFarPlaneDistance() { return far; }
        public void setFarPlaneDistance(float value) { far = value; }
    }
}
