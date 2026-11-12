package com.dragonminez.common.stats.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/** Bounded appearance proportions shared by creation, saves and server validation. */
public final class BodyShape {
    public enum Part { HEIGHT, HEAD, LEGS, ARMS, TORSO_HEIGHT, TORSO_WIDTH, LEG_WIDTH, ARM_WIDTH, HEAD_WIDTH, HEAD_DEPTH, HEAD_HEIGHT, TORSO_DEPTH, ARM_DEPTH, LEG_DEPTH }
    private final float[] values = new float[Part.values().length];
    public BodyShape() { java.util.Arrays.fill(values,1); }
    // -1 follows the skin; 0 Steve; 1 Alex.
    private int frame = -1;
    public float get(Part part) { return values[part.ordinal()]; }
    public void set(Part part, float value) { values[part.ordinal()] = Float.isFinite(value) ? Mth.clamp(value,.5f,1.5f) : 1; }
    public int frame() { return frame; }
    public void frame(int value) { frame = Mth.clamp(value,-1,1); }
    public float heightRatio() { return (12*get(Part.LEGS)+12*get(Part.TORSO_HEIGHT)+8*get(Part.HEAD)*get(Part.HEAD_HEIGHT))/32; }
    public float widthRatio() { return Math.max(get(Part.TORSO_WIDTH),get(Part.HEAD)*get(Part.HEAD_WIDTH)); }
    public BodyShape copy() { return load(save()); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Part part:Part.values()) tag.putFloat(part.name(),get(part));
        tag.putInt("Frame",frame);
        return tag;
    }
    public static BodyShape load(CompoundTag tag) {
        BodyShape shape = new BodyShape();
        if (tag == null) return shape;
        for (Part part:Part.values()) if (tag.contains(part.name())) shape.set(part,tag.getFloat(part.name()));
        if (tag.contains("Frame")) shape.frame(tag.getInt("Frame"));
        return shape;
    }
    public void write(FriendlyByteBuf buffer) {
        for (Part part:Part.values()) buffer.writeFloat(get(part));
        buffer.writeByte(frame);
    }
    public static BodyShape read(FriendlyByteBuf buffer) {
        BodyShape shape = new BodyShape();
        for (Part part:Part.values()) shape.set(part,buffer.readFloat());
        shape.frame(buffer.readByte());
        return shape;
    }
}
