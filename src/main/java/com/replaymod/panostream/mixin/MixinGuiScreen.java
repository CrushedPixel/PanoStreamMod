package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.CaptureState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    //renders a flat background texture instead of a gradient so it tiles better in 360°
    @Redirect(method = "drawWorldBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawGradientRect(IIIIII)V"))
    private void renderFlatGradient(GuiScreen gs, int i1, int i2, int i3, int i4, int i5, int i6) {
        if(!CaptureState.isCapturing()) {
            drawGradientRect(0, 0, gs.width, gs.height, 0xc0101010, 0xd0101010);
        } else {
            GuiScreen.drawRect(0, 0, gs.width, gs.height, 0xc0101010);
        }
    }

    /**
     * Draws a rectangle with a vertical gradient between the specified colors (ARGB format). Args : x1, y1, x2, y2,
     * topColor, bottomColor
     */
    //NOTE: for some reason, extending GuiScreen doesn't work in the latest mixin version, so we copy this method as a dirty workaround
    //TODO: find a solution for this, as we currently assume zLevel to be 0 at all times
    protected void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float zLevel = 0;

        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos((double)right, (double)top, zLevel).color(f1, f2, f3, f).endVertex();
        vertexbuffer.pos((double)left, (double)top, zLevel).color(f1, f2, f3, f).endVertex();
        vertexbuffer.pos((double)left, (double)bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        vertexbuffer.pos((double)right, (double)bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}
