package com.replaymod.panostream.capture.vr180;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.Frame;
import com.replaymod.panostream.capture.FrameCapturer;
import com.replaymod.panostream.capture.Program;
import com.replaymod.panostream.capture.ZeroPassFrame;
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
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.GlStateManager.BooleanState;
import static net.minecraft.client.renderer.GlStateManager.enableDepth;

public class VR180FrameCapturer extends FrameCapturer {

    /**
     * Resource locations of the VR180 shader
     */
    private static final ResourceLocation VERTEX_SHADER = new ResourceLocation("panostream", "vr180.vert");
    private static final ResourceLocation TESSELLATION_CONTROL_SHADER = new ResourceLocation("panostream", "vr180.tesc");
    private static final ResourceLocation TESSELLATION_EVALUATION_SHADER = new ResourceLocation("panostream", "vr180.tese");
    private static final ResourceLocation GEOMETRY_SHADER = new ResourceLocation("panostream", "vr180.geom");
    private static final ResourceLocation FRAGMENT_SHADER = new ResourceLocation("panostream", "vr180.frag");

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private static VR180FrameCapturer active;

    // Current (but not necessarily active) frame capturer
    @Getter
    private static VR180FrameCapturer current;

    private int frameSize;
    private VR180Frame vr180Frame;
    private ZeroPassFrame zeroPassFrame;
    private Frame singlePassFrame;

    private List<Program> programs = new ArrayList<>();
    /**
     * Shader which performs tessellation depending on apparent size of rendered quads using a GL32 geometry shader.
     */
    private Program geomTessProgram;
    private Program geomTessOverlayProgram; // GUI variant
    /**
     * Basic VR180 shader with no tessellation at all.
     */
    private Program simpleProgram;
    private Program simpleOverlayProgram; // GUI variant

    private Program boundProgram;
    private int renderDistance;
    private boolean zeroPass;
    private boolean singlePass;
    private boolean tessellation;
    private boolean overlay;
    private boolean leftEye;
    private BooleanState[] previousStates = new BooleanState[3];
    private BooleanState previousFogState;
    private Framebuffer mcFramebufferBefore;
    private int mouseX, mouseY;

    public VR180FrameCapturer(int frameSize, int fps, VideoStreamer videoStreamer) {
        super(fps, videoStreamer);
        this.frameSize = frameSize;

        recreateFrame();
        loadPrograms();

        current = this;
    }

    public void recreateFrame() {
        zeroPass = GuiDebug.instance.zeroPass;
        singlePass = !zeroPass && GuiDebug.instance.singlePass;

        if (vr180Frame != null) {
            vr180Frame.destroy();
            vr180Frame = null;
        }
        if (zeroPassFrame != null) {
            zeroPassFrame.destroy();
            zeroPassFrame = null;
        }
        if (singlePassFrame != null) {
            singlePassFrame.destroy();
            singlePassFrame = null;
        }
        if (zeroPass) {
            zeroPassFrame = new ZeroPassFrame(frameSize, 2 * frameSize);
        } else if (singlePass) {
            singlePassFrame = new Frame(frameSize, 2 * frameSize, true);
        } else {
            vr180Frame = new VR180Frame(frameSize, false);
        }
    }

    private void loadPrograms() {
        // initialize VR180 shader
        try {
            renderDistance = mc.gameSettings.renderDistanceChunks * 16;

            boolean multiPass = !zeroPass && !singlePass;
            String defines = "";
            defines += GuiDebug.instance.markLeftEye ? "#define MARK_LEFT_EYE\n" : "";
            defines += "#define FAR_PLANE_DISTANCE " + renderDistance * MathHelper.SQRT_2 + "\n";
            defines += zeroPass ? "#define ZERO_PASS\n" : "";
            defines += singlePass ? "#define SINGLE_PASS\n" : "";
            defines += !multiPass && GuiDebug.instance.geometryShaderInstancing ? "#define GS_INSTANCING\n" : "";
            defines += "#define MAX_TESS_LEVEL " + GuiDebug.instance.maxTessLevel + "\n";
            ResourceLocation geometryShader =
                    !multiPass
                            && GuiDebug.instance.geometryShaderInstancing
                            && GuiDebug.instance.alwaysUseGeometryShaderInstancing ? GEOMETRY_SHADER : null;
            defines += geometryShader != null ? "#define WITH_GS 1\n" : "";
            defines += !multiPass && GuiDebug.instance.drawInstanced ? "#define DRAW_INSTANCED\n" : "";

            if (GuiDebug.instance.tessellationShader) {
                programs.add(geomTessProgram = new Program(
                        VERTEX_SHADER,
                        TESSELLATION_CONTROL_SHADER,
                        TESSELLATION_EVALUATION_SHADER,
                        geometryShader,
                        FRAGMENT_SHADER,
                        "#define WITH_TES 1\n" + defines));
                programs.add(geomTessOverlayProgram = new Program(
                        VERTEX_SHADER,
                        TESSELLATION_CONTROL_SHADER,
                        TESSELLATION_EVALUATION_SHADER,
                        geometryShader,
                        FRAGMENT_SHADER,
                        "#define WITH_TES 1\n#define OVERLAY 1\n" + defines));
            } else {
                programs.add(geomTessProgram = new Program(VERTEX_SHADER, GEOMETRY_SHADER, FRAGMENT_SHADER,
                        "#define WITH_GS 1\n" + defines));
                programs.add(geomTessOverlayProgram = new Program(VERTEX_SHADER, GEOMETRY_SHADER, FRAGMENT_SHADER,
                        "#define WITH_GS 1\n#define OVERLAY 1\n" + defines));
            }
            programs.add(simpleProgram = new Program(VERTEX_SHADER, geometryShader, FRAGMENT_SHADER,
                    "#define NO_TESSELLATION 1\n" + defines));
            programs.add(simpleOverlayProgram = new Program(VERTEX_SHADER, geometryShader, FRAGMENT_SHADER,
                    "#define NO_TESSELLATION 1\n#define OVERLAY 1\n" + defines));

            for (Program program : programs) {
                program.use();
                program.getUniformVariable("texture").set(0);
                program.getUniformVariable("lightMap").set(1);
                program.stopUsing();
            }

        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating VR180 shader"));
        }
    }

    public void reloadPrograms() {
        if (boundProgram != null) throw new IllegalStateException();
        for (Program program : programs) {
            program.delete();
        }
        programs.clear();
        loadPrograms();
    }

    private Program getTessellationProgram() {
        return overlay ? geomTessOverlayProgram : geomTessProgram;
    }

    private Program getSimpleProgram() {
        return overlay ? simpleOverlayProgram : simpleProgram;
    }

    public boolean isTessellationActive() {
        return tessellation;
    }

    public void setTessellationActive(boolean active) {
        tessellation = active;
    }

    public void enableTessellation() {
        setTessellationActive(true);
    }

    public void disableTessellation() {
        setTessellationActive(false);
    }

    public void forceLazyRenderState() {
        Program tessellationProgram = getTessellationProgram();
        if (tessellation && boundProgram != tessellationProgram) {
            bindProgram(tessellationProgram);
        }
        Program simpleProgram = getSimpleProgram();
        if (!tessellation && boundProgram != simpleProgram) {
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

            if (zeroPass) {
                float width = zeroPassFrame.getComposedFramebuffer().framebufferWidth;
                float height = zeroPassFrame.getComposedFramebuffer().framebufferHeight;
                program.setUniformValue("inverseViewportAspect", height / width);
                program.setUniformValue("mcAspect", (float) mc.displayWidth / mc.displayHeight);
                program.setUniformValue("mcWidthFraction", mc.displayWidth / width);
                program.setUniformValue("mcHeightFraction", mc.displayHeight / height);
                program.setUniformValue("eyeWidthFraction", frameSize / width);
                program.setUniformValue("eyeHeightFraction", frameSize / height);
            } else if (!singlePass) {
                program.uniforms().renderPass.set(leftEye ? 0 : 1);
            }
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
        if (zeroPass) {
            return doCaptureZeroPass();
        } else {
            return doCapture(true);
        }
    }

    private void prePass() {
        active = this;
        CaptureState.setCapturing(true);
        CaptureState.setOrientation(EquirectangularFrameCapturer.Orientation.FRONT);
        if (zeroPass || singlePass) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
            if (zeroPass) {
                GL11.glEnable(GL11.GL_CLIP_PLANE1);
            }
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        }
        if (GuiDebug.instance.wireframe) {
            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GlStateManager.glLineWidth(2.0F);
        }

        ScaledResolution userResolution = new ScaledResolution(mc);
        mouseX = Mouse.getX() * userResolution.getScaledWidth() / mc.displayWidth;
        mouseY = userResolution.getScaledHeight() - Mouse.getY() * userResolution.getScaledHeight() / mc.displayHeight;
    }

    private void postPass() {
        bindProgram(null);

        active = null;
        CaptureState.setCapturing(false);
        if (zeroPass || singlePass) {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            if (zeroPass) {
                GL11.glDisable(GL11.GL_CLIP_PLANE1);
            }
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        }
        if (GuiDebug.instance.wireframe) {
            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
    }

    //
    //  Zero-Pass Mode
    //

    @Override
    protected void beginFrame() {
        if (renderDistance != mc.gameSettings.renderDistanceChunks * 16) {
            reloadPrograms();
        }

        if (!zeroPass) return;

        zeroPassFrame.updateSize();
        zeroPassFrame.getComposedFramebuffer().bindFramebuffer(true);
        prePass();

        // temporarily replace Minecraft's framebuffer with our framebuffer as GuiMainMenu explicitly binds it
        mcFramebufferBefore = mc.framebuffer;
        mc.framebuffer = zeroPassFrame.getComposedFramebuffer();

        GlStateManager.clearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        overlay = false;
        enableTessellation();

        GuiDebug.instance.queryWorldLeft.begin();
        if (mc.world == null) {
            postRenderWorld();
        }
    }

    public void postRenderWorld() {
        if (!zeroPass) return;

        GuiDebug.instance.queryWorldLeft.end();

        GuiDebug.instance.queryWorldRight.begin();
        GuiDebug.instance.queryWorldRight.end();

        mc.framebuffer = mcFramebufferBefore;
        postPass();

        GuiDebug.instance.queryCompose.begin();
        zeroPassFrame.blitToMC();
        GuiDebug.instance.queryCompose.end();
    }

    private ByteBuffer doCaptureZeroPass() {
        assert zeroPass;

        zeroPassFrame.getComposedFramebuffer().bindFramebuffer(true);
        prePass();

        overlay = true;
        if (GuiDebug.instance.tessellateGui) {
            enableTessellation();
        } else {
            disableTessellation();
        }
        GuiDebug.instance.queryGuiLeft.begin();
        renderOverlays(true, mouseX, mouseY);
        GuiDebug.instance.queryGuiLeft.end();

        postPass();
        Frame.unbindFramebuffer();

        GuiDebug.instance.queryGuiRight.begin();
        GuiDebug.instance.queryGuiRight.end();

        GuiDebug.instance.queryTransfer.begin();
        ByteBuffer frame = zeroPassFrame.getByteBuffer();
        GuiDebug.instance.queryTransfer.end();

        return frame;
    }

    //
    //  One-/Multi-Pass Mode
    //

    protected ByteBuffer doCapture(boolean flip) {
        assert !zeroPass;

        // render left eye
        leftEye = true;
        if (boundProgram != null) {
            boundProgram.uniforms().renderPass.set(0);
        }
        if (singlePass) {
            singlePassFrame.getComposedFramebuffer().bindFramebuffer(true);
        } else {
            vr180Frame.bindFramebuffer(true);
        }
        prePass();
        GuiDebug.instance.queryWorldLeft.begin();
        renderWorld();
        GuiDebug.instance.queryWorldLeft.end();

        GuiDebug.instance.queryGuiLeft.begin();
        renderOverlays(true, mouseX, mouseY);
        GuiDebug.instance.queryGuiLeft.end();

        Frame.unbindFramebuffer();

        bindProgram(null);

        // render right eye
        leftEye = false;
        if (boundProgram != null) {
            boundProgram.uniforms().renderPass.set(1);
        }
        if (!singlePass) {
            vr180Frame.bindFramebuffer(false);
        }

        GuiDebug.instance.queryWorldRight.begin();
        if (!singlePass) {
            renderWorld();
        }
        GuiDebug.instance.queryWorldRight.end();

        GuiDebug.instance.queryGuiRight.begin();
        if (!singlePass) {
            renderOverlays(false, mouseX, mouseY);
        }
        GuiDebug.instance.queryGuiRight.end();

        if (!singlePass) {
            Frame.unbindFramebuffer();
        }

        postPass();

        GuiDebug.instance.queryCompose.begin();
        if (!singlePass) {
            vr180Frame.composeTopBottom(flip);
        }
        GuiDebug.instance.queryCompose.end();

        GuiDebug.instance.queryTransfer.begin();
        ByteBuffer frame = (singlePass ? singlePassFrame : vr180Frame).getByteBuffer();
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
        enableTessellation();

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    private void renderOverlays(boolean left, int mouseX, int mouseY) {
        if (this.mc.gameSettings.hideGUI && this.mc.currentScreen == null || !GuiDebug.instance.renderGui) return;

        overlay = true;
        if (GuiDebug.instance.tessellateGui) {
            enableTessellation();
        } else {
            disableTessellation();
        }

        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        // We disable depth testing in the GUI since MC seems to rely on EQ to pass which we cannot guarantee.
        // Even though this isn't the default, less GUIs are broken this way and those that are can be manually fixed
        //disableDepth();

        // temporarily replace Minecraft's framebuffer with our framebuffer as GuiMainMenu explicitly binds it
        Framebuffer before = mc.framebuffer;
        try {
            mc.framebuffer = zeroPass ? zeroPassFrame.getComposedFramebuffer() : singlePass ? singlePassFrame.getComposedFramebuffer() : vr180Frame.getFramebuffer(left);

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

    public Framebuffer getComposedFramebuffer() {
        return (zeroPass ? zeroPassFrame : singlePass ? singlePassFrame : vr180Frame).getComposedFramebuffer();
    }

    public boolean isZeroPass() {
        return zeroPass;
    }

    public boolean isSinglePass() {
        return singlePass;
    }

    @Override
    public void destroy() {
        geomTessProgram.delete();
        simpleProgram.delete();
        if (vr180Frame != null) {
            vr180Frame.destroy();
        }
        if (zeroPassFrame != null) {
            zeroPassFrame.destroy();
        }
        if (singlePassFrame != null) {
            singlePassFrame.destroy();
        }

        if (current == this) {
            current = null;
        }
    }

    @Override
    public VR180FrameCapturer getThis() {
        return this;
    }

    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }
}
