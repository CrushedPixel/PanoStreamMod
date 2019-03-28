package com.replaymod.panostream.capture;

import com.replaymod.panostream.gui.GuiDebug;
import com.replaymod.panostream.utils.ByteBufferPool;
import lombok.Getter;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

public class Frame {

    /**
     * The framebuffer holding the composed frame.
     */
    @Getter
    protected final Framebuffer composedFramebuffer;

    private int width, height;
    private PixelBufferObject pbos[];
    private int ready;

    public Frame(int width, int height) {
        this(width, height, false);
    }

    public Frame(int width, int height, boolean useDepth) {
        // initialize the framebuffer
        composedFramebuffer = new Framebuffer(width, height, useDepth);
        composedFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 1.0F);

        // initialize the PBO array (PBOs are initialized in resize())
        pbos = new PixelBufferObject[GuiDebug.instance.pbos];

        resize(width, height);
    }

    public void resize(int width, int height) {
        if (pbos[0] != null) {
            destroy();
        }

        this.width = width;
        this.height = height;

        composedFramebuffer.createFramebuffer(width, height);

        int bufferSize = width * height * 4;
        for (int i = 0; i < pbos.length; i++) {
            pbos[i] = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        }

        ready = -pbos.length;
    }

    /**
     * Transfers the composed framebuffer to the PBO.
     */
    private void transferToPBO() {
        pbos[0].bind();

        if (GuiDebug.instance.useReadPixels) {
            composedFramebuffer.bindFramebuffer(true);
            GL11.glReadBuffer(OpenGlHelper.GL_COLOR_ATTACHMENT0);
            GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0);
            composedFramebuffer.unbindFramebuffer();
        } else {
            composedFramebuffer.bindFramebufferTexture();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0);
            composedFramebuffer.unbindFramebufferTexture();
        }

        pbos[0].unbind();
    }

    private void shiftPBOs() {
        PixelBufferObject last = pbos[pbos.length - 1];
        System.arraycopy(pbos, 0, pbos, 1, pbos.length - 1);
        pbos[0] = last;
    }

    /**
     * Reads the current composed framebuffer into a ByteBuffer.
     */
    public ByteBuffer getByteBuffer() {
        if (!GuiDebug.instance.transfer) return ByteBufferPool.allocate(pbos[0].getSize());

        transferToPBO();
        shiftPBOs();

        ready++;
        if (ready < 0) {
            return null;
        }

        // read the PBO's contents into a ByteBuffer
        pbos[0].bind();
        ByteBuffer pboBuffer = pbos[0].mapReadOnly();
        ByteBuffer buffer = ByteBufferPool.allocate(pbos[0].getSize());
        buffer.put(pboBuffer);
        buffer.rewind();
        pbos[0].unmap();
        pbos[0].unbind();
        return buffer;
    }

    public void destroy() {
        for (PixelBufferObject pbo : pbos) {
            if (pbo == null) continue;
            pbo.delete();
        }
        composedFramebuffer.deleteFramebuffer();
    }

    public static void unbindFramebuffer() {
        if (OpenGlHelper.isFramebufferEnabled()) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        }
    }
}
