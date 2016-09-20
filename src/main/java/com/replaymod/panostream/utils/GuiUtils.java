package com.replaymod.panostream.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class GuiUtils {

    public static void drawRotatedRectWithCustomSizedTexture(int x, int y, float rotation, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        GlStateManager.pushMatrix();

        float f4 = 1.0F / textureWidth;
        float f5 = 1.0F / textureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer worldrenderer = tessellator.getBuffer();
        GlStateManager.translate(x+(width/2), y+(width/2), 0);
        GlStateManager.rotate(rotation, 0, 0, 1);
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-width / 2, height / 2, 0.0D).tex((double) (u * f4), (double) ((v + (float) height) * f5)).endVertex();
        worldrenderer.pos(width / 2, height / 2, 0.0D).tex((double) ((u + (float) width) * f4), (double) ((v + (float) height) * f5)).endVertex();
        worldrenderer.pos(width / 2, -height / 2, 0.0D).tex((double) ((u + (float) width) * f4), (double) (v * f5)).endVertex();
        worldrenderer.pos(-width / 2, -height / 2, 0.0D).tex((double) (u * f4), (double) (v * f5)).endVertex();
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        fontRenderer.drawStringWithShadow(text, (float) (x - fontRenderer.getStringWidth(text) / 2), (float) y, color);
    }

}
