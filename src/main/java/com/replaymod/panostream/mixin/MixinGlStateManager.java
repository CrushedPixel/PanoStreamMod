package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
    private static boolean inGlNewList;

    /**
     * @reason Geometry shader do not support GL_QUADS, so we use GL_LINES_ADJACENCY instead and transform those into two
     *         triangles in the geometry shader.
     *         Same for the tessellation shader except we use GL_PATCHES.
     *         Or, if we in single-pass mode, draw twice (unless we're also using GS Instancing).
     * @author johni0702
     */
    @Overwrite
    public static void glDrawArrays(int mode, int first, int count) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null && !inGlNewList) {
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
                && !dbg.drawInstanced
                && !(dbg.alwaysUseGeometryShaderInstancing || (CaptureState.isGeometryShader() && dbg.geometryShaderInstancing))) {
            Program program = Program.getBoundProgram();
            program.uniforms().renderPass.set(0);
            GL11.glDrawArrays(mode, first, count);
            dbg.drawCallCounter++;
            program.uniforms().renderPass.set(1);
            if (capturer.isZeroPass()) {
                GL11.glDrawArrays(mode, first, count);
                dbg.drawCallCounter++;
                program.uniforms().renderPass.set(2);
            }
        }
        if (capturer != null && (capturer.isZeroPass() || capturer.isSinglePass()) && dbg != null && dbg.drawInstanced) {
            if (inGlNewList) {
                return; // glDrawArraysInstanced cannot be used in display lists
            }
            GL31.glDrawArraysInstanced(mode, first, count, capturer.isZeroPass() ? 3 : 2);
        } else {
            GL11.glDrawArrays(mode, first, count);
        }
        if (capturer != null && dbg != null) dbg.drawCallCounter++;
    }

    @Inject(method = "glNewList", at = @At("HEAD"))
    private static void glNewList(int list, int mode, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        inGlNewList = true;
    }

    @Inject(method = "glEndList", at = @At("HEAD"))
    private static void glEndList(CallbackInfo ci) {
        inGlNewList = false;
    }

    @Inject(method = "glBegin", at = @At("HEAD"))
    private static void forceLazyState(int mode, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null && !inGlNewList) {
            capturer.forceLazyRenderState();
        }
    }

    @Redirect(method = "viewport", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V", remap = false))
    private static void singlePassViewport(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null
                && (capturer.isZeroPass() || capturer.isSinglePass())
                && x == 0
                && y == 0
                && width == mc.displayWidth
                && height == mc.displayHeight) {
            width = capturer.getComposedFramebuffer().framebufferWidth;
            height = capturer.getComposedFramebuffer().framebufferHeight;
        }
        GL11.glViewport(x, y, width, height);
    }

    @Shadow
    private static void cullFace(int mode) {}

    /**
     * @reason In single-pass mode, we flip all verticies vertically and as such need to flip the cull face.
     * @author johni0702
     */
    @Overwrite
    public static void cullFace(GlStateManager.CullFace cullFace) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null && (capturer.isZeroPass() || capturer.isSinglePass())) {
            switch (cullFace) {
                case FRONT:
                    cullFace = GlStateManager.CullFace.BACK;
                    break;
                case BACK:
                    cullFace = GlStateManager.CullFace.FRONT;
                    break;
                case FRONT_AND_BACK:
                    cullFace = GlStateManager.CullFace.FRONT_AND_BACK;
                    break;
            }
        }
        cullFace(cullFace.mode);
    }
}
