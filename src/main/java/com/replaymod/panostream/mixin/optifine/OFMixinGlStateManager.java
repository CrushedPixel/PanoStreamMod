package com.replaymod.panostream.mixin.optifine;

import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

@Mixin(GlStateManager.class)
public class OFMixinGlStateManager {
    /**
     * @reason same as MixinGlStateManager#glDrawArrays but for Optifine's "Render Regions" feature
     * @author johni0702
     */
    @Dynamic
    @Overwrite(remap = false)
    public static void glMultiDrawArrays(int mode, IntBuffer first, IntBuffer count) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        GuiDebug dbg = GuiDebug.instance;

        if (CaptureState.isTessEvalShader()) {
            if (mode != GL11.GL_QUADS) return; // TES can only draw quads
            GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 4);
            mode = GL40.GL_PATCHES;
        } else if (CaptureState.isGeometryShader()) {
            if (mode != GL11.GL_QUADS) return; // GS can only draw quads
            mode = GL32.GL_LINES_ADJACENCY;
        }

        if (capturer != null
                && (capturer.isZeroPass() || capturer.isSinglePass())
                && dbg != null
                && !(dbg.alwaysUseGeometryShaderInstancing || (CaptureState.isGeometryShader() && dbg.geometryShaderInstancing))) {
            Program program = Program.getBoundProgram();
            program.uniforms().renderPass.set(0);
            GL14.glMultiDrawArrays(mode, first, count);
            dbg.drawCallCounter++;
            program.uniforms().renderPass.set(1);
            if (capturer.isZeroPass()) {
                GL14.glMultiDrawArrays(mode, first, count);
                dbg.drawCallCounter++;
                program.uniforms().renderPass.set(2);
            }
        }
        GL14.glMultiDrawArrays(mode, first, count);
        if (capturer != null && dbg != null) dbg.drawCallCounter++;
    }
}
