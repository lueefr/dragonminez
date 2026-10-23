package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class RenderNormals {
    private RenderNormals() {}
    public static VertexConsumer set(VertexConsumer consumer, Matrix3f matrix, float x, float y, float z) {
        Vector3f normal = matrix.transform(new Vector3f(x, y, z));
        return consumer.setNormal(normal.x(), normal.y(), normal.z());
    }
}
