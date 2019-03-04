package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {
    // Decreases zLevel during drawing of the container background to fix incorrect z layering
    @Inject(method = "drawContainerBackground", at = @At("HEAD"), remap = false, cancellable = true)
    private void bindVR180Shader(Tessellator tessellator, CallbackInfo ci) {
        if (CaptureState.isGeometryShader()) {
            GlStateManager.translate(0f, 0f, -500f);
        }
    }

    @Inject(method = "drawContainerBackground", at = @At("RETURN"), remap = false)
    private void resetZLevel(Tessellator tessellator, CallbackInfo ci) {
        if (CaptureState.isGeometryShader()) {
            GlStateManager.translate(0f, 0f, 500f);
        }
    }
}
