package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame extends Gui {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void disableVignette(float f, ScaledResolution res, CallbackInfo ci) {
        if(CaptureState.isCapturing()) ci.cancel();
    }

    @Redirect(method = "renderAttackIndicator", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", ordinal = 0))
    private void renderCrossHairWithDepth(GuiIngame guiIngame, int x, int y, int textureX, int textureY, int width, int height) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (!CaptureState.isCapturing() || !CaptureState.isGeometryShader() && !CaptureState.isTessEvalShader()) {
            guiIngame.drawTexturedModalRect(x, y, textureX, textureY, width, height);
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            if (capturer != null) {
                capturer.setOverlay(false);
                capturer.forceLazyRenderState();
            }

            float oldZLevel = zLevel;
            zLevel = 0;
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();

            mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2);

            Vec3d hitPos = mc.objectMouseOver.hitVec;
            Vec3d cameraPos = mc.getRenderViewEntity().getPositionEyes(mc.timer.renderPartialTicks);
            double dist = hitPos.distanceTo(cameraPos);
            GlStateManager.translate(0.0, mc.getRenderViewEntity().getEyeHeight(), 0.0);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1f, 0f, 0f);
            GlStateManager.translate(0.0, 0.0, dist);
            double size = dist / 16 / 12;
            GlStateManager.scale(size, size, size);

            guiIngame.drawTexturedModalRect(-width / 2, -height / 2, textureX, textureY, width, height);

            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            zLevel = oldZLevel;

            if (capturer != null) {
                capturer.setOverlay(true);
            }
        }
    }
}
