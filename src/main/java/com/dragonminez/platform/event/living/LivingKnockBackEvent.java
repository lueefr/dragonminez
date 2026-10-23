package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
public final class LivingKnockBackEvent extends LivingEvent implements ICancellableEvent {
    private float strength;
    private double ratioX, ratioZ;
    public LivingKnockBackEvent(LivingEntity entity, double strength, double x, double z) {
        super(entity); this.strength = (float) strength; ratioX = x; ratioZ = z;
    }
    public float getStrength() { return strength; }
    public void setStrength(float strength) { this.strength = strength; }
    public double getRatioX() { return ratioX; }
    public double getRatioZ() { return ratioZ; }
    public void setRatioX(double ratio) { ratioX = ratio; }
    public void setRatioZ(double ratio) { ratioZ = ratio; }
}
