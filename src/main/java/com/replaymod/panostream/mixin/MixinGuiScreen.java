package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen extends Gui {

    //renders a flat background texture instead of a gradient so it tiles better in 360°
    // also renders the background at a lower zLevel (relevant for tessellation in vr180)
    @Redirect(method = "drawWorldBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawGradientRect(IIIIII)V"))
    private void renderFlatGradient(GuiScreen gs, int i1, int i2, int i3, int i4, int i5, int i6) {
        if(!CaptureState.isCapturing()) {
            drawGradientRect(0, 0, gs.width, gs.height, 0xc0101010, 0xd0101010);
        } else {
            zLevel -= 10000;
            drawGradientRect(0, 0, gs.width, gs.height, 0xc0101010, 0xc0101010);
            zLevel += 10000;
        }
    }

    // Decreases zLevel during drawing of the container background to fix incorrect z layering
    @Inject(method = "drawBackground", at = @At("HEAD"))
    private void setZLevel(int tint, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (CaptureState.isGeometryShader()) {
            GlStateManager.translate(0f, 0f, -10000f);
        }
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void resetZLevel(int tint, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (CaptureState.isGeometryShader()) {
            GlStateManager.translate(0f, 0f, 10000f);
        }
    }
}
