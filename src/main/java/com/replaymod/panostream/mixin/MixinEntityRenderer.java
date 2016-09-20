package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.CaptureState;
import com.replaymod.panostream.utils.ScaledResolutionUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void setupCubicFrameRotation(float partialTicks, CallbackInfo ci) {
        if(!CaptureState.isCapturing()) return;

        switch(CaptureState.getOrientation()) {
            case FRONT:
                GlStateManager.rotate(0, 0.0F, 1.0F, 0.0F);
                break;
            case RIGHT:
                GlStateManager.rotate(90, 0.0F, 1.0F, 0.0F);
                break;
            case BACK:
                GlStateManager.rotate(180, 0.0F, 1.0F, 0.0F);
                break;
            case LEFT:
                GlStateManager.rotate(-90, 0.0F, 1.0F, 0.0F);
                break;
            case BOTTOM:
                GlStateManager.rotate(90, 1.0F, 0.0F, 0.0F);
                break;
            case TOP:
                GlStateManager.rotate(-90, 1.0F, 0.0F, 0.0F);
                break;
        }

        //undoing the glTranslate call in the orientCamera method
        GlStateManager.translate(0.0F, 0.0F, 0.1F);
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$0(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$1(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$2(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "setupOverlayRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;ortho(DDDDDD)V"))
    private void centerOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        if(!CaptureState.isCapturing() || !CaptureState.isDistortGUI()) {
            GlStateManager.ortho(left, right, bottom, top, zNear, zFar);
        } else {
            ScaledResolution sc = ScaledResolutionUtil.createScaledResolution(Display.getWidth(), Display.getHeight());

            if(sc.getScaledWidth_double() > sc.getScaledHeight_double()) {
                double diff = sc.getScaledWidth_double() - sc.getScaledHeight_double();
                GlStateManager.ortho(diff/2, sc.getScaledHeight_double() + (diff/2), sc.getScaledHeight_double(), 0, zNear, zFar);
            } else {
                double diff = sc.getScaledHeight_double() - sc.getScaledWidth_double();
                GlStateManager.ortho(0, sc.getScaledWidth_double(), sc.getScaledWidth_double() + (diff/2), diff/2, zNear, zFar);
            }
        }
    }

    public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        //normalizing the FOV for capturing of cubic frames
        if(CaptureState.isCapturing()) {
            fovY = 90;
            aspect = 1;
        }
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }
}
