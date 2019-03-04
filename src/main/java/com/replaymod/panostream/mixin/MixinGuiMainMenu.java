package com.replaymod.panostream.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends Gui {
    // Decreases zLevel during drawing of the background panorama to fix incorrect z layering
    @Inject(method = "renderSkybox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebuffer(Z)V"))
    private void setZLevel(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        zLevel -= 100;
    }

    @Inject(method = "renderSkybox", at = @At("RETURN"))
    private void resetZLevel(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        zLevel += 100;
    }
}
