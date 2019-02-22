package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen extends Gui {

    //renders a flat background texture instead of a gradient so it tiles better in 360°
    @Redirect(method = "drawWorldBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawGradientRect(IIIIII)V"))
    private void renderFlatGradient(GuiScreen gs, int i1, int i2, int i3, int i4, int i5, int i6) {
        if(!CaptureState.isCapturing()) {
            drawGradientRect(0, 0, gs.width, gs.height, 0xc0101010, 0xd0101010);
        } else {
            GuiScreen.drawRect(0, 0, gs.width, gs.height, 0xc0101010);
        }
    }

}
