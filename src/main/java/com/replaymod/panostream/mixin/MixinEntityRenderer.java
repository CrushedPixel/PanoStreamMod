package com.replaymod.panostream.mixin;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.PanoramicFrameCapturer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void setupCubicFrameRotation(float partialTicks, CallbackInfo ci) {
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getPanoramicFrameCapturer();
        if(!capturer.isActive() || capturer.getOrientation() == null) return;

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

        // Minecraft goes back a little so we have to invert that
        GlStateManager.translate(0.0F, 0.0F, 0.1F);
    }

}
