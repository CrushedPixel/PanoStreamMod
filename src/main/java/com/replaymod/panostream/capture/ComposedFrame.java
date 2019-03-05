package com.replaymod.panostream.capture;

import com.replaymod.panostream.gui.GuiDebug;
import com.replaymod.panostream.utils.ByteBufferPool;
import lombok.Getter;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public abstract class ComposedFrame {

    /**
     * The framebuffer holding the composed frame.
     */
    @Getter
    protected final Framebuffer composedFramebuffer;

    private PixelBufferObject pbo;

    protected ComposedFrame(int width, int height) {
        // initialize the framebuffer
        composedFramebuffer = new Framebuffer(width, height, false);
        composedFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

        // initialize the PBO
        int bufferSize = width * height * 3;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    /**
     * Transfers the composed framebuffer to the PBO.
     */
    private void transferToPBO() {
        pbo.bind();

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        composedFramebuffer.bindFramebufferTexture();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
        composedFramebuffer.unbindFramebufferTexture();

        pbo.unbind();
    }

    /**
     * Reads the current composed framebuffer into a ByteBuffer.
     */
    public ByteBuffer getByteBuffer() {
        if (!GuiDebug.instance.transfer) return ByteBufferPool.allocate(pbo.getSize());

        transferToPBO();

        // read the PBO's contents into a ByteBuffer
        pbo.bind();
        ByteBuffer pboBuffer = pbo.mapReadOnly();
        ByteBuffer buffer = ByteBufferPool.allocate(pbo.getSize());
        buffer.put(pboBuffer);
        buffer.rewind();
        pbo.unmap();
        pbo.unbind();

        return buffer;
    }

    public void destroy() {
        pbo.delete();
        composedFramebuffer.deleteFramebuffer();
    }

    public static void unbindFramebuffer() {
        if (OpenGlHelper.isFramebufferEnabled()) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        }
    }
}
