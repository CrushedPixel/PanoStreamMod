package com.replaymod.panostream;

import lombok.Getter;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

//a panoramic frame consists out of six framebuffers
//one for each direction.
public class PanoramicFrame {

    private Framebuffer[] framebuffers = new Framebuffer[6];

    @Getter
    private int frameSize;

    public PanoramicFrame(int frameSize) {
        this.frameSize = frameSize;

        for(int i=0; i<6; i++) {
            Framebuffer fb = new Framebuffer(frameSize, frameSize, true);
            fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            fb.unbindFramebuffer();

            framebuffers[i] = fb;
        }
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;

        for(int i=0; i<6; i++) {
            framebuffers[i].createFramebuffer(frameSize, frameSize);
        }
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
}
