package com.replaymod.panostream.capture.vr180;

import com.replaymod.panostream.capture.ComposedFrame;
import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.gui.GuiDebug;
import lombok.Getter;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class VR180Frame extends ComposedFrame {

    /**
     * Resource locations of the composition shader
     */
    private static final ResourceLocation VERTEX_SHADER = new ResourceLocation("panostream", "tb_stereo.vert");
    private static final ResourceLocation FRAGMENT_SHADER = new ResourceLocation("panostream", "tb_stereo.frag");

    private Framebuffer leftEye, rightEye;

    @Getter
    private final int frameSize;

    private final Program shaderProgram;

    public VR180Frame(int frameSize) {
        super(frameSize, 2 * frameSize);

        this.frameSize = frameSize;

        leftEye = initFramebuffer();
        rightEye = initFramebuffer();

        // initialize composition shader
        try {
            shaderProgram = new Program(VERTEX_SHADER, FRAGMENT_SHADER);
            shaderProgram.use();

            shaderProgram.setUniformValue("leftEyeTex", 2);
            shaderProgram.setUniformValue("rightEyeTex", 3);

            shaderProgram.stopUsing();

        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating top-bottom composition shader"));
        }
    }

    private Framebuffer initFramebuffer() {
        Framebuffer fb = new Framebuffer(frameSize, frameSize, true);
        fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        return fb;
    }

    public Framebuffer getFramebuffer(boolean left) {
        return left ? leftEye : rightEye;
    }

    public void bindFramebuffer(boolean left) {
        getFramebuffer(left).bindFramebuffer(true);
    }

    public void composeTopBottom(boolean flip) {
        if (!GuiDebug.instance.compose) return;
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
        leftEye.bindFramebufferTexture();
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE3);
        rightEye.bindFramebufferTexture();

        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE4);
        composedFramebuffer.bindFramebuffer(true);

        shaderProgram.use();
        shaderProgram.setUniformValue("flip", flip ? 1 : 0);

        composedFramebuffer.framebufferRender(frameSize, frameSize * 2);

        shaderProgram.stopUsing();

        composedFramebuffer.unbindFramebuffer();

        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0); // TODO: needed?

        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
        leftEye.unbindFramebufferTexture();
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE3);
        rightEye.unbindFramebufferTexture();

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public void destroy() {
        super.destroy();

        shaderProgram.delete();

        leftEye.deleteFramebuffer();
        rightEye.deleteFramebuffer();
    }
}
