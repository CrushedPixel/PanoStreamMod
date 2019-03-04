package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
    /**
     * @reason Geometry shader do not support GL_QUADS, so we use GL_LINES_ADJACENCY instead and transform those into two
     *         triangles in the geometry shader.
     * @author johni0702
     */
    @Overwrite
    public static void glDrawArrays(int mode, int first, int count) {
        if (CaptureState.isGeometryShader()) {
            // Geometry shader is active, so we can only draw quads and must discard everything else
            if (mode == GL11.GL_QUADS) {
                GL11.glDrawArrays(GL32.GL_LINES_ADJACENCY, first, count);
            }
        } else {
            GL11.glDrawArrays(mode, first, count);
        }
    }
}
