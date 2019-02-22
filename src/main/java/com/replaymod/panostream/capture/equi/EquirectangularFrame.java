package com.replaymod.panostream.capture.equi;

import com.replaymod.panostream.capture.ComposedFrame;
import com.replaymod.panostream.capture.Program;
import lombok.Getter;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * An EquirectangularFrame consists out of six framebuffers,
 * one for each direction.
 */
public class EquirectangularFrame extends ComposedFrame {
    
    /**
     * Resource locations of the composition shader
     */
    private static final ResourceLocation VERTEX_SHADER = new ResourceLocation("panostream", "equi.vert");
    private static final ResourceLocation FRAGMENT_SHADER = new ResourceLocation("panostream", "equi.frag");

    private Framebuffer[] framebuffers = new Framebuffer[6];

    @Getter
    private final int frameSize;

    private final Program shaderProgram;

    public EquirectangularFrame(int frameSize) {
        super(frameSize * 4, frameSize * 2);

        this.frameSize = frameSize;

        for (int i = 0; i < 6; i++) {
            Framebuffer fb = new Framebuffer(frameSize, frameSize, true);
            fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

            framebuffers[i] = fb;
        }

        // initialize composition shader
        try {
            shaderProgram = new Program(VERTEX_SHADER, FRAGMENT_SHADER);

            shaderProgram.use();

            shaderProgram.setUniformValue("frontTex", 2);
            shaderProgram.setUniformValue("backTex", 3);
            shaderProgram.setUniformValue("leftTex", 4);
            shaderProgram.setUniformValue("rightTex", 5);
            shaderProgram.setUniformValue("bottomTex", 6);
            shaderProgram.setUniformValue("topTex", 7);

            shaderProgram.stopUsing();

        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating equirectangular composition shader"));
        }
    }

    public Framebuffer getFramebuffer(int i) {
        return framebuffers[i];
    }

    public void bindFramebuffer(int i) {
        framebuffers[i].bindFramebuffer(true);
    }

    public void composeEquirectangular(boolean flip, float yawCorrection, float pitchCorrection) {
        for (int i = 0; i < 6; i++) {
            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2 + i);
            framebuffers[i].bindFramebufferTexture();
        }

        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE8);

        composedFramebuffer.bindFramebuffer(true);

        shaderProgram.use();

        shaderProgram.setUniformValue("flip", flip ? 1 : 0);
        shaderProgram.setUniformValue("yawCorrection", yawCorrection);
        shaderProgram.setUniformValue("pitchCorrection", pitchCorrection);

        composedFramebuffer.framebufferRender(frameSize * 4, frameSize * 2);

        shaderProgram.stopUsing();

        composedFramebuffer.unbindFramebuffer();

        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);

        for (int i = 0; i < 6; i++) {
            OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2 + i);
            framebuffers[i].unbindFramebufferTexture();
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public void destroy() {
        super.destroy();

        shaderProgram.delete();

        // delete framebuffers
        for (int i = 0; i < 6; i++) {
            framebuffers[i].deleteFramebuffer();
        }
    }
}
