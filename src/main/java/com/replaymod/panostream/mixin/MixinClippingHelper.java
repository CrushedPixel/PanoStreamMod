package com.replaymod.panostream.mixin;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.equi.CaptureState;
import net.minecraft.client.renderer.culling.ClippingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClippingHelper.class)
public class MixinClippingHelper {

    @Inject(method = "isBoxInFrustum", at = @At("HEAD"), cancellable = true)
    public void forceRenderChunksWhileStreaming(double d0, double d1, double d2, double d3, double d4, double d5, CallbackInfoReturnable<Boolean> ci) {
        if (PanoStreamMod.instance.getVideoStreamer().getStreamingThread().isActive() || CaptureState.isCapturing()) {
            ci.setReturnValue(true);
            ci.cancel();
        }
    }

}
