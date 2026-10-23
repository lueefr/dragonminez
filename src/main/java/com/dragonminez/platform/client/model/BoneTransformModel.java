package com.dragonminez.platform.client.model;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
public interface BoneTransformModel {
    void adjustBones(RenderPassInfo<?> pass, BoneSnapshots snapshots);
}
