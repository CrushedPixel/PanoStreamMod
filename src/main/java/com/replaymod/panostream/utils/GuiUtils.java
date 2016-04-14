package com.replaymod.panostream.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

public class GuiUtils {

    public static void drawRotatedRectWithCustomSizedTexture(int x, int y, float rotation, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        GlStateManager.pushMatrix();

        float f4 = 1.0F / textureWidth;
        float f5 = 1.0F / textureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.translate(x+(width/2), y+(width/2), 0);
        GlStateManager.rotate(rotation, 0, 0, 1);
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertexWithUV(-width / 2, height / 2, 0.0D, (double) (u * f4), (double) ((v + (float) height) * f5));
        worldrenderer.addVertexWithUV(width/2, height/2, 0.0D, (double)((u + (float)width) * f4), (double)((v + (float)height) * f5));
        worldrenderer.addVertexWithUV(width/2, -height/2, 0.0D, (double)((u + (float)width) * f4), (double)(v * f5));
        worldrenderer.addVertexWithUV(-width/2, -height/2, 0.0D, (double)(u * f4), (double)(v * f5));
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        fontRenderer.drawStringWithShadow(text, (float) (x - fontRenderer.getStringWidth(text) / 2), (float) y, color);
    }

}
