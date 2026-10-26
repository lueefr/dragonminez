package com.dragonminez.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Restores the caller's stencil state, including Minecraft's cached front-face state. */
public final class StencilState implements AutoCloseable {
    private final boolean enabled;
    private final Face front;
    private final Face back;

    private record Face(int writeMask, int function, int reference, int valueMask,
                        int fail, int depthFail, int depthPass) {}

    private StencilState() {
        RenderSystem.assertOnRenderThread();
        enabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        front = new Face(GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK),
                GL11.glGetInteger(GL11.GL_STENCIL_FUNC), GL11.glGetInteger(GL11.GL_STENCIL_REF),
                GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK), GL11.glGetInteger(GL11.GL_STENCIL_FAIL),
                GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL), GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS));
        back = new Face(GL11.glGetInteger(GL20.GL_STENCIL_BACK_WRITEMASK),
                GL11.glGetInteger(GL20.GL_STENCIL_BACK_FUNC), GL11.glGetInteger(GL20.GL_STENCIL_BACK_REF),
                GL11.glGetInteger(GL20.GL_STENCIL_BACK_VALUE_MASK), GL11.glGetInteger(GL20.GL_STENCIL_BACK_FAIL),
                GL11.glGetInteger(GL20.GL_STENCIL_BACK_PASS_DEPTH_FAIL), GL11.glGetInteger(GL20.GL_STENCIL_BACK_PASS_DEPTH_PASS));
    }

    public static StencilState capture() {
        return new StencilState();
    }

    @Override
    public void close() {
        // RenderSystem updates GlStateManager's cache as well as both native faces.
        RenderSystem.stencilMask(front.writeMask());
        RenderSystem.stencilFunc(front.function(), front.reference(), front.valueMask());
        RenderSystem.stencilOp(front.fail(), front.depthFail(), front.depthPass());
        // Other renderers can use raw GL calls without updating Minecraft's cache.
        // Restore native state explicitly even when RenderSystem considers a value unchanged.
        restoreFace(GL11.GL_FRONT, front);
        restoreFace(GL11.GL_BACK, back);
        if (enabled) GL11.glEnable(GL11.GL_STENCIL_TEST);
        else GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void restoreFace(int face, Face state) {
        GL20.glStencilMaskSeparate(face, state.writeMask());
        GL20.glStencilFuncSeparate(face, state.function(), state.reference(), state.valueMask());
        GL20.glStencilOpSeparate(face, state.fail(), state.depthFail(), state.depthPass());
    }
}
