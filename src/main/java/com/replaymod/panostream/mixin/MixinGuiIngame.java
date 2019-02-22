package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void disableVignette(float f, ScaledResolution res, CallbackInfo ci) {
        if(CaptureState.isCapturing()) ci.cancel();
    }

}
