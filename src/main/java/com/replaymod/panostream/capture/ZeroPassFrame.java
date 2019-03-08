package com.replaymod.panostream.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class ZeroPassFrame extends Frame {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int frameWidth, frameHeight, mcWidth, mcHeight;

    public ZeroPassFrame(int frameWidth, int frameHeight) {
        super(frameWidth, frameHeight, true);

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;

        updateSize();
    }

    public void resize(int width, int height) {
        frameWidth = width;
        frameHeight = height;

        super.resize(width, height);

        mcWidth = 0;
        mcHeight = 0;
        updateSize();
    }

    public void updateSize() {
        if (mc.displayWidth != mcWidth || mc.displayHeight != mcHeight) {
            mcWidth = mc.displayWidth;
            mcHeight = mc.displayHeight;

            int width = Math.max(frameWidth, mcWidth);
            int height = frameHeight + mcHeight;
            getComposedFramebuffer().createBindFramebuffer(width, height);
        }
    }

    public void blitToMC() {
        if (mc.framebuffer == getComposedFramebuffer()) {
            throw new IllegalStateException("The MC framebuffer must not be the framebuffer of this frame.");
        }
        OpenGlHelper.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mc.framebuffer.framebufferObject);
        OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, getComposedFramebuffer().framebufferObject);
        GL30.glBlitFramebuffer(
                0, frameHeight, mcWidth, frameHeight + mcHeight,
                0, mcHeight, mcWidth, 0,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
        );
        OpenGlHelper.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}
