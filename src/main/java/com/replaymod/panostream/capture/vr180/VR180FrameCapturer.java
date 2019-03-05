package com.replaymod.panostream.capture.vr180;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.ComposedFrame;
import com.replaymod.panostream.capture.FrameCapturer;
import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.equi.EquirectangularFrameCapturer;
import com.replaymod.panostream.gui.GuiDebug;
import com.replaymod.panostream.stream.VideoStreamer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.BooleanState;
import static net.minecraft.client.renderer.GlStateManager.enableDepth;

public class VR180FrameCapturer extends FrameCapturer {

    /**
     * Resource locations of the VR180 shader
     */
    private static final ResourceLocation VERTEX_SHADER = new ResourceLocation("panostream", "vr180.vert");
    private static final ResourceLocation GEOMETRY_SHADER = new ResourceLocation("panostream", "vr180.geom");
    private static final ResourceLocation FRAGMENT_SHADER = new ResourceLocation("panostream", "vr180.frag");

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private static VR180FrameCapturer active;

    // Current (but not necessarily active) frame capturer
    @Getter
    private static VR180FrameCapturer current;

    private int frameSize;
    protected VR180Frame vr180Frame;

    /**
     * Shader which performs tessellation depending on apparent size of rendered quads using a GL32 geometry shader.
     */
    private Program geomTessProgram;
    /**
     * Basic VR180 shader with no tessellation at all.
     */
    private Program simpleProgram;

    private boolean lazyEnableTessellation;
    private boolean lazyDisableTessellation;

    private Program boundProgram;
    private boolean overlay;
    private boolean leftEye;
    private BooleanState[] previousStates = new BooleanState[3];
    private BooleanState previousFogState;

    public VR180FrameCapturer(int frameSize, int fps, VideoStreamer videoStreamer) {
        super(fps, videoStreamer);
        this.frameSize = frameSize;

        recreateFrame();
        loadPrograms();

        current = this;
    }

    public void recreateFrame() {
        if (vr180Frame != null) {
            vr180Frame.destroy();
            vr180Frame = null;
        }
        vr180Frame = new VR180Frame(frameSize);
    }

    private void loadPrograms() {
        // initialize VR180 shader
        try {
            double localFov = Math.PI / 8; /*(90 / 2) * Math.PI / 180*/;
            double aspect = 0.5;

            double meshFov = (140 / 2) * Math.PI / 180;
            double tanLocalFov = Math.tan(localFov);
            double remoteFov = (100 / 2) * Math.PI / 180;
            double tanRemoteFov = Math.tan(remoteFov);

            double theta = aspect * tanLocalFov * meshFov;
            double phi = tanLocalFov / meshFov;
            double zed = aspect * tanRemoteFov;

            geomTessProgram = new Program(VERTEX_SHADER, GEOMETRY_SHADER, FRAGMENT_SHADER,
                    "#define GS 1\n");
            geomTessProgram.use();
            geomTessProgram.getUniformVariable("thetaFactor").set((float) theta);
            geomTessProgram.getUniformVariable("phiFactor").set((float) phi);
            geomTessProgram.getUniformVariable("zedFactor").set((float) zed);
            geomTessProgram.getUniformVariable("texture").set(0);
            geomTessProgram.getUniformVariable("lightMap").set(1);
            geomTessProgram.stopUsing();

            simpleProgram = new Program(VERTEX_SHADER, FRAGMENT_SHADER);
            simpleProgram.use();
            simpleProgram.getUniformVariable("thetaFactor").set((float) theta);
            simpleProgram.getUniformVariable("phiFactor").set((float) phi);
            simpleProgram.getUniformVariable("zedFactor").set((float) zed);
            simpleProgram.getUniformVariable("texture").set(0);
            simpleProgram.getUniformVariable("lightMap").set(1);
            simpleProgram.stopUsing();

        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating VR180 shader"));
        }
    }

    @SuppressWarnings("unused") // to be called from debugger
    private void reloadPrograms() {
        if (boundProgram != null) throw new IllegalStateException();
        if (geomTessProgram != null) {
            geomTessProgram.delete();
            geomTessProgram = null;
        }
        if (simpleProgram != null) {
            simpleProgram.delete();
            simpleProgram = null;
        }
        loadPrograms();
    }

    public boolean isTessellationActive() {
        return lazyEnableTessellation || (!lazyDisableTessellation && boundProgram == geomTessProgram);
    }

    public void setTessellationActive(boolean active) {
        if (active) {
            enableTessellation();
        } else {
            disableTessellation();
        }
    }

    public void enableTessellation() {
        lazyEnableTessellation = true;
        lazyDisableTessellation = false;
    }

    public void disableTessellation() {
        lazyEnableTessellation = false;
        lazyDisableTessellation = true;
    }

    public void forceLazyRenderState() {
        if (lazyEnableTessellation && boundProgram != geomTessProgram) {
            bindProgram(geomTessProgram);
        }
        if (lazyDisableTessellation && boundProgram != simpleProgram) {
            bindProgram(simpleProgram);
        }
    }

    private void bindProgram(Program program) {
        // Note: must not short circuit in case boundProgram == program (or if it does, it must update all uniforms)
        if (boundProgram != null) {
            // unhook BooleanStates
            for (int i = 0; i < previousStates.length; i++) {
                previousStates[i].currentState = GlStateManager.textureState[i].texture2DState.currentState;
                GlStateManager.textureState[i].texture2DState = previousStates[i];
            }
            previousFogState.currentState = GlStateManager.fogState.fog.currentState;
            GlStateManager.fogState.fog = previousFogState;

            boundProgram.stopUsing();
            boundProgram = null;
        }
        if (program != null) {
            GuiDebug.instance.programSwitchesCounter++;
            program.use();

            program.getUniformVariable("overlay").set(overlay);
            program.getUniformVariable("leftEye").set(leftEye);
            program.getUniformVariable("ipd").set(PanoStreamMod.instance.getPanoStreamSettings().ipd.getValue().floatValue());

            // link the GlStateManager's BooleanStates to the fragment shader's uniforms
            linkState(program, 0, "textureEnabled");
            linkState(program, 1, "lightMapEnabled");
            linkState(program, 2, "hurtTextureEnabled");

            // link the fog state
            final Program.Uniform fogUniform = program.getUniformVariable("fogEnabled");
            previousFogState = GlStateManager.fogState.fog;
            fogUniform.set(previousFogState.currentState);
            GlStateManager.fogState.fog = new GlStateManager.BooleanState(previousFogState.capability) {
                { currentState = previousFogState.currentState; }

                @Override
                public void setState(boolean state) {
                    super.setState(state);
                    fogUniform.set(state);
                }
            };

            boundProgram = program;
        }
    }

    private void linkState(Program program, int id, String var) {
        final Program.Uniform uniform = program.getUniformVariable(var);
        previousStates[id] = GlStateManager.textureState[id].texture2DState;
        uniform.set(previousStates[id].currentState);

        GlStateManager.textureState[id].texture2DState = new BooleanState(previousStates[id].capability) {
            { currentState =  previousStates[id].currentState; }

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

        active = this;
        CaptureState.setCapturing(true);
        CaptureState.setOrientation(EquirectangularFrameCapturer.Orientation.FRONT);
        GuiDebug.instance.programSwitchesCounter = 0;

        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;

        ScaledResolution userResolution = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * userResolution.getScaledWidth() / mc.displayWidth;
        int mouseY = userResolution.getScaledHeight() - Mouse.getY() * userResolution.getScaledHeight() / mc.displayHeight;

        mc.displayWidth = mc.displayHeight = vr180Frame.getFrameSize();

        // render left eye
        leftEye = true;
        vr180Frame.bindFramebuffer(true);

        GuiDebug.instance.queryWorldLeft.begin();
        renderWorld();
        GuiDebug.instance.queryWorldLeft.end();

        GuiDebug.instance.queryGuiLeft.begin();
        renderOverlays(true, mouseX, mouseY);
        GuiDebug.instance.queryGuiLeft.end();

        ComposedFrame.unbindFramebuffer();

        // render right eye
        leftEye = false;
        vr180Frame.bindFramebuffer(false);

        GuiDebug.instance.queryWorldRight.begin();
        renderWorld();
        GuiDebug.instance.queryWorldRight.end();

        GuiDebug.instance.queryGuiRight.begin();
        renderOverlays(false, mouseX, mouseY);
        GuiDebug.instance.queryGuiRight.end();

        ComposedFrame.unbindFramebuffer();

        bindProgram(null);

        // restore mc size
        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        active = null;
        CaptureState.setCapturing(false);

        GuiDebug.instance.queryCompose.begin();
        vr180Frame.composeTopBottom(flip);
        GuiDebug.instance.queryCompose.end();

        GuiDebug.instance.queryTransfer.begin();
        ByteBuffer frame = vr180Frame.getByteBuffer();
        GuiDebug.instance.queryTransfer.end();

        return frame;
    }

    private void renderWorld() {
        // TODO: DRY with EquirectangularFrameCapturer if possible (base class method?)
        if (mc.world == null || !GuiDebug.instance.renderWorld) {
            GlStateManager.clearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            return;
        }

        // render the world with as little overweight function calls as possible
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        overlay = false;
        bindProgram(geomTessProgram); // explicit re-bind to update uniforms
        enableTessellation();

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    private void renderOverlays(boolean left, int mouseX, int mouseY) {
        if (this.mc.gameSettings.hideGUI && this.mc.currentScreen == null || !GuiDebug.instance.renderGui) return;

        overlay = true;
        if (GuiDebug.instance.tessellateGui) {
            bindProgram(geomTessProgram); // explicit re-bind to update uniforms
            enableTessellation();
        } else {
            bindProgram(simpleProgram); // explicit re-bind to update uniforms
            disableTessellation();
        }

        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        // We disable depth testing in the GUI since MC seems to rely on EQ to pass which we cannot guarantee.
        // Even though this isn't the default, less GUIs are broken this way and those that are can be manually fixed
        //disableDepth();

        // temporarily replace Minecraft's framebuffer with our framebuffer as GuiMainMenu explicitly binds it
        Framebuffer before = mc.framebuffer;
        try {
            mc.framebuffer = vr180Frame.getFramebuffer(left);

            if (mc.player != null) mc.ingameGUI.renderGameOverlay(mc.timer.renderPartialTicks);
            if (mc.currentScreen != null) {
                CaptureState.setDistortGUI(true);
                mc.entityRenderer.setupOverlayRendering(); //re-setup overlay rendering with distortion enabled
                CaptureState.setDistortGUI(false);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                ForgeHooksClient.drawScreen(mc.currentScreen, mouseX, mouseY, mc.timer.renderPartialTicks);
            }
        } finally {
            mc.framebuffer = before;
            enableDepth();
        }
    }

    @Override
    public void destroy() {
        geomTessProgram.delete();
        simpleProgram.delete();
        vr180Frame.destroy();

        if (current == this) {
            current = null;
        }
    }

    @Override
    public VR180FrameCapturer getThis() {
        return this;
    }
}
