package com.replaymod.panostream.mixin;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.PanoramicFrameCapturer;
import net.minecraft.client.Minecraft;
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
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getVideoStreamer().getPanoramicFrameCapturer();
        if(capturer == null || !capturer.isActive() || capturer.getOrientation() == null) return;

        switch(capturer.getOrientation()) {
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
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getVideoStreamer().getPanoramicFrameCapturer();
        if(capturer == null || !capturer.isActive() || !capturer.isDistortGUI()) {
            GlStateManager.ortho(left, right, bottom, top, zNear, zFar);
        } else {
            int w = Display.getWidth();
            int h = Display.getHeight();

            ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), w, h);

            int width = sc.getScaledWidth();
            int height = sc.getScaledHeight();

            double newLeft = left;
            double newRight = right;
            double newTop = top;
            double newBottom = bottom;

            if(width > height) {
                //the GUI is wider than 1:1
                double newWidth = right / (right / width);
                newLeft = -((right - newWidth));

                newBottom = bottom / (bottom / height); //THIS IS CORRECT!
            } else {
                //the GUI is taller than 1:1
                double newHeight = bottom / (bottom / height);
                newTop = -((bottom - newHeight));

                newRight = right / (right / width); //THIS IS CORRECT!
            }

            GlStateManager.ortho(newLeft, newRight, newBottom, newTop, zNear, zFar);
        }
    }

    public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getVideoStreamer().getPanoramicFrameCapturer();
        //normalizing the FOV for capturing of cubic frames
        if(capturer != null && capturer.isActive() && capturer.getOrientation() != null) {
            fovY = 90;
            aspect = 1;
        }
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }

    /* To rotate the hand for other frames, I'll have to figure out where to inject those calls
    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderOverlays(F)V", ordinal = 0, shift = At.Shift.BY, by = 0))
    private void doRenderHand(float someFloat, int someInt, CallbackInfo ci) {
        //rotateHand();
    }
    */

    private void rotateHand() {
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getVideoStreamer().getPanoramicFrameCapturer();

        if(capturer != null && capturer.isActive() && capturer.getOrientation() != null) {
            switch(capturer.getOrientation()) {
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
        }
    }
}
