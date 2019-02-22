package com.replaymod.panostream.capture.vr180;

import com.replaymod.panostream.capture.ComposedFrame;
import com.replaymod.panostream.capture.FrameCapturer;
import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.equi.EquirectangularFrameCapturer;
import com.replaymod.panostream.stream.VideoStreamer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;

import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.BooleanState;

public class VR180FrameCapturer extends FrameCapturer {

    /**
     * Resource locations of the VR180 shader
     */
    private static final ResourceLocation VERTEX_SHADER = new ResourceLocation("panostream", "vr180.vert");
    private static final ResourceLocation FRAGMENT_SHADER = new ResourceLocation("panostream", "vr180.frag");

    private final Minecraft mc = Minecraft.getMinecraft();

    protected final VR180Frame vr180Frame;

    private final Program shaderProgram;

    private BooleanState[] previousStates = new BooleanState[3];
    private BooleanState previousFogState;

    private final Program.Uniform thetaFactor, phiFactor, zedFactor;

    public VR180FrameCapturer(int frameSize, int fps, VideoStreamer videoStreamer) {
        super(fps, videoStreamer);
        vr180Frame = new VR180Frame(frameSize);

        // initialize VR180 shader
        try {
            shaderProgram = new Program(VERTEX_SHADER, FRAGMENT_SHADER);
            shaderProgram.use();

            double localFov = Math.PI / 8; /*(90 / 2) * Math.PI / 180*/;
            double aspect = 0.5;

            double meshFov = (140 / 2) * Math.PI / 180;
            double tanLocalFov = Math.tan(localFov);
            double remoteFov = (100 / 2) * Math.PI / 180;
            double tanRemoteFov = Math.tan(remoteFov);

            double theta = aspect * tanLocalFov * meshFov;
            double phi = tanLocalFov / meshFov;
            double zed = aspect * tanRemoteFov * -1;

            thetaFactor = shaderProgram.getUniformVariable("thetaFactor");
            thetaFactor.set((float) theta);

            phiFactor = shaderProgram.getUniformVariable("phiFactor");
            phiFactor.set((float) phi);

            zedFactor = shaderProgram.getUniformVariable("zedFactor");
            zedFactor.set((float) zed);

            System.out.println(theta);
            System.out.println(phi);
            System.out.println(zed);

            /*
            auto fov = XM_PIDIV4;
            auto aspect = (width / 2.f) / (FLOAT)height;
            auto near_clip = 0.1f;
            auto far_clip = 100.f;
            g_Projection = XMMatrixPerspectiveFovLH( fov, aspect, near_clip, far_clip );

            auto mesh_fov = 70.f * XM_PI / 180.f;
            auto tan_local_fov = tanf(fov / 2.f);
            auto remote_fov = 50.f * XM_PI / 180.f;
            auto tan_remote_fov = tanf(remote_fov);

            CBChangeOnResize cbChangesOnResize;
            cbChangesOnResize.mProjection = ::XMMatrixTranspose( g_Projection );
            cbChangesOnResize.mThetaFactor = aspect * tan_local_fov / mesh_fov;
            cbChangesOnResize.mPhiFactor = tan_local_fov / mesh_fov;
            cbChangesOnResize.mZedFactor = aspect * tan_remote_fov;
            g_pImmediateContext->UpdateSubresource( g_pCBChangeOnResize, 0, nullptr, &cbChangesOnResize, 0, 0 );
            */

            shaderProgram.getUniformVariable("texture").set(0);
            shaderProgram.getUniformVariable("lightMap").set(1);

            shaderProgram.stopUsing();

        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating VR180 shader"));
        }
    }

    private void linkState(int id, String var) {
        final Program.Uniform uniform = shaderProgram.getUniformVariable(var);
        previousStates[id] = GlStateManager.textureState[id].texture2DState;
        uniform.set(previousStates[id].currentState);

        GlStateManager.textureState[id].texture2DState = new BooleanState(previousStates[id].capability) {
            @Override
            public void setState(boolean state) {
                super.setState(state);
                uniform.set(state);
            }
        };
    }

    @Override
    protected ByteBuffer captureFrame() {
        return doCapture(true);
    }

    protected ByteBuffer doCapture(boolean flip) {
        if (vr180Frame == null) return null;

        CaptureState.setCapturing(true);
        CaptureState.setOrientation(EquirectangularFrameCapturer.Orientation.FRONT);

        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;

        mc.displayWidth = mc.displayHeight = vr180Frame.getFrameSize();

        // use the VR180 shader
        shaderProgram.use();

        // link the GlStateManager's BooleanStates to the fragment shader's uniforms
        linkState(0, "textureEnabled");
        linkState(1, "lightMapEnabled");
        linkState(2, "hurtTextureEnabled");

        // link the fog state
        final Program.Uniform fogUniform = shaderProgram.getUniformVariable("fogEnabled");
        previousFogState = GlStateManager.fogState.fog;
        fogUniform.set(previousFogState.currentState);
        GlStateManager.fogState.fog = new GlStateManager.BooleanState(previousFogState.capability) {
            @Override
            public void setState(boolean state) {
                super.setState(state);
                fogUniform.set(state);
            }
        };

        // render left eye
        vr180Frame.bindFramebuffer(true);
        renderWorld();
        // TODO: render overlays
        ComposedFrame.unbindFramebuffer();

        // render right eye
        // TODO: move camera by IPD
        vr180Frame.bindFramebuffer(false);
        renderWorld();
        ComposedFrame.unbindFramebuffer();

        // unhook BooleanStates
        for (int i = 0; i < previousStates.length; i++) {
            GlStateManager.textureState[i].texture2DState = previousStates[i];
        }
        GlStateManager.fogState.fog = previousFogState;

        shaderProgram.stopUsing();

        // restore mc size
        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        CaptureState.setCapturing(false);

        vr180Frame.composeTopBottom(flip);
        return vr180Frame.getByteBuffer();
    }

    private void renderWorld() {
        // TODO: DRY with EquirectangularFrameCapturer if possible (base class method?)
        if (mc.world == null) return;
        // render the world with as little overweight function calls as possible
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    @Override
    public void destroy() {
        shaderProgram.delete();
        vr180Frame.destroy();
    }

    @Override
    public VR180FrameCapturer getThis() {
        return this;
    }
}
