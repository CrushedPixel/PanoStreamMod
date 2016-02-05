package com.replaymod.panostream.capture;

import com.replaymod.panostream.shader.Program;
import com.replaymod.panostream.utils.ByteBufferPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;

//a panoramic frame consists out of six framebuffers
//one for each direction.
public class PanoramicFrame {
    private static final ResourceLocation vertexResource = new ResourceLocation("panostream", "equi.vert");
    private static final ResourceLocation fragmentResource = new ResourceLocation("panostream", "equi.frag");

    private Framebuffer[] framebuffers = new Framebuffer[6];

    @Getter
    private Framebuffer composedFramebuffer;

    @Getter
    private PixelBufferObject pbo;

    @Getter
    private int frameSize, bufferSize;

    private Program shaderProgram;

    public PanoramicFrame(int frameSize) {
        this.frameSize = frameSize;

        for(int i=0; i<6; i++) {
            Framebuffer fb = new Framebuffer(frameSize, frameSize, true);
            fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

            framebuffers[i] = fb;
        }

        composedFramebuffer = new Framebuffer(frameSize*4, frameSize*2, false);
        composedFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

        try {
            shaderProgram = new Program(vertexResource, fragmentResource);

            shaderProgram.use();

            shaderProgram.setTexture("frontTex", 2);
            shaderProgram.setTexture("backTex", 3);
            shaderProgram.setTexture("leftTex", 4);
            shaderProgram.setTexture("rightTex", 5);
            shaderProgram.setTexture("bottomTex", 6);
            shaderProgram.setTexture("topTex", 7);

            shaderProgram.stopUsing();
        } catch(Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating Equirectangular shaders"));
        }

        bufferSize = (frameSize * 4) * (frameSize * 2) * 3;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;

        for(int i=0; i<6; i++) {
            framebuffers[i].createFramebuffer(frameSize, frameSize);
        }

        if(pbo != null) pbo.delete();

        bufferSize = (frameSize * 4) * (frameSize * 2) * 3;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    public Framebuffer getFramebuffer(int i) {
        return framebuffers[i];
    }

    public void bindFramebuffer(int i) {
        framebuffers[i].bindFramebuffer(true);
    }

    public void deleteFramebuffers() {
        for(int i=0; i<6; i++) {
            framebuffers[i].deleteFramebuffer();
        }
    }

    public void unbindFramebuffer() {
        if (OpenGlHelper.isFramebufferEnabled()) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        }
    }

    public void composeEquirectangular() {
        for(int i=0; i<6; i++) {
            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2 + i);
            framebuffers[i].bindFramebufferTexture();
        }

        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE8);

        composedFramebuffer.bindFramebuffer(true);

        shaderProgram.use();
        
        composedFramebuffer.framebufferRender(frameSize * 4, frameSize * 2);

        shaderProgram.stopUsing();

        composedFramebuffer.unbindFramebuffer();

        for(int i=0; i<6; i++) {
            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2 + i);
            framebuffers[i].unbindFramebufferTexture();
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        //for development purposes
        if(GuiScreen.isCtrlKeyDown())
            ScreenShotHelper.saveScreenshot(Minecraft.getMinecraft().mcDataDir, 0, 0, composedFramebuffer);
    }

    public void transferToPBO() {
        pbo.bind();

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        composedFramebuffer.bindFramebufferTexture();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
        composedFramebuffer.unbindFramebufferTexture();

        pbo.unbind();
    }

    public ByteBuffer getByteBuffer() {
        transferToPBO();

        //read the PBO's contents into a ByteBuffer
        pbo.bind();
        ByteBuffer pboBuffer = pbo.mapReadOnly();
        ByteBuffer buffer = ByteBufferPool.allocate(bufferSize);
        buffer.put(pboBuffer);
        buffer.rewind();
        pbo.unmap();
        pbo.unbind();

        return buffer;
    }
}
