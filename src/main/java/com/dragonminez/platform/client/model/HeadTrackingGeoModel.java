package com.dragonminez.platform.client.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Captures head tracking once and applies it through GeckoLib 5 bone snapshots. */
public abstract class HeadTrackingGeoModel<T extends GeoAnimatable> extends ExtractedGeoModel<T> implements BoneTransformModel {
    private record HeadPose(float pitch, float yaw, boolean enabled, boolean additive) {}
    private static final DataTicket<HeadPose> HEAD_POSE = DataTickets.create("dragonminez:head_pose", HeadPose.class);
    protected boolean trackHead(T animatable) { return true; }
    protected float pitchLimit() { return Float.POSITIVE_INFINITY; }
    protected float yawLimit() { return Float.POSITIVE_INFINITY; }
    protected boolean additiveHeadRotation() { return false; }
    @Override public void addAdditionalStateData(T animatable, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(animatable, relatedObject, state);
        if (!(state instanceof LivingEntityRenderState living)) throw new IllegalArgumentException("Head tracking requires a living entity render state");
        state.addGeckolibData(HEAD_POSE, new HeadPose(
                Mth.clamp(living.xRot, -pitchLimit(), pitchLimit()) * Mth.DEG_TO_RAD,
                Mth.clamp(living.yRot, -yawLimit(), yawLimit()) * Mth.DEG_TO_RAD,
                trackHead(animatable), additiveHeadRotation()));
    }
    @Override public void adjustBones(RenderPassInfo<?> pass, BoneSnapshots snapshots) {
        HeadPose pose = pass.getGeckolibData(HEAD_POSE);
        if (pose == null || !pose.enabled()) return;
        snapshots.ifPresent("head", head -> {
            head.setRotX((pose.additive() ? head.getRotX() : 0) + pose.pitch());
            head.setRotY((pose.additive() ? head.getRotY() : 0) + pose.yaw());
        });
    }
}
