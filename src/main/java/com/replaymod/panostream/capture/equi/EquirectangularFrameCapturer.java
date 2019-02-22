package com.replaymod.panostream.capture.equi;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.ComposedFrame;
import com.replaymod.panostream.capture.FrameCapturer;
import com.replaymod.panostream.gui.EmptyGuiScreen;
import com.replaymod.panostream.stream.VideoStreamer;
import com.replaymod.panostream.utils.ScaledResolutionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;


public class EquirectangularFrameCapturer extends FrameCapturer {

    private final Minecraft mc = Minecraft.getMinecraft();

    protected final EquirectangularFrame equirectangularFrame;

    private final EmptyGuiScreen emptyGuiScreen = new EmptyGuiScreen();

    public EquirectangularFrameCapturer(int frameSize, int fps, VideoStreamer videoStreamer) {
        super(fps, videoStreamer);

        equirectangularFrame = new EquirectangularFrame(frameSize);
        ScaledResolutionUtil.setWorldAndResolution(emptyGuiScreen, frameSize, frameSize);
    }

    @Override
    protected ByteBuffer captureFrame() {
        return doCapture(true, PanoStreamMod.instance.getPanoStreamSettings().stabilizeOutput.getValue());
    }

    @Override
    public void destroy() {
        equirectangularFrame.destroy();
    }

    protected ByteBuffer doCapture(boolean flip, boolean stabilize) {
        if (equirectangularFrame == null) return null;

        CaptureState.setCapturing(true);
        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;

        // calculate the mouse location on the distorted gui
        ScaledResolution actualResolution = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * actualResolution.getScaledWidth() / widthBefore;
        int mouseY = actualResolution.getScaledHeight() - Mouse.getY() * actualResolution.getScaledHeight() / heightBefore - 1;

        mc.displayWidth = mc.displayHeight = equirectangularFrame.getFrameSize();

        for (int i = 0; i < 6; i++) {
            CaptureState.setOrientation(Orientation.values()[i]);

            equirectangularFrame.bindFramebuffer(i);

            renderWorld();
            renderOverlays(mouseX, mouseY);

            ComposedFrame.unbindFramebuffer();
        }

        float yawCorrection = 0;
        float pitchCorrection = 0;

        Entity viewEntity = mc.getRenderViewEntity();
        if (stabilize && viewEntity != null) {
            yawCorrection = (float) Math.toRadians(viewEntity.rotationYaw);
            pitchCorrection = (float) Math.toRadians(viewEntity.rotationPitch);
        }

        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        CaptureState.setCapturing(false);

        equirectangularFrame.composeEquirectangular(flip, -yawCorrection, -pitchCorrection);
        return equirectangularFrame.getByteBuffer();
    }

    private void renderWorld() {
        if (mc.world == null) return;
        // render the world with as little overweight function calls as possible
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    private void renderOverlays(int mouseX, int mouseY) {
        if (this.mc.gameSettings.hideGUI && this.mc.currentScreen == null) return;

        // if a GuiScreen is opened, render the default overlay on the other sides
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        if (CaptureState.getOrientation() != Orientation.FRONT) {
            if (mc.currentScreen != null) {
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                mc.entityRenderer.setupOverlayRendering();
                ForgeHooksClient.drawScreen(emptyGuiScreen, 0, 0, 0);
            }
        } else {
            // temporarily replace Minecraft's framebuffer with our framebuffer as GuiMainMenu explicitly binds it
            Framebuffer before = mc.framebuffer;
            mc.framebuffer = equirectangularFrame.getFramebuffer(0);

            if (mc.player != null) mc.ingameGUI.renderGameOverlay(mc.timer.renderPartialTicks);
            if (mc.currentScreen != null) {
                CaptureState.setDistortGUI(true);
                mc.entityRenderer.setupOverlayRendering(); //re-setup overlay rendering with distortion enabled
                CaptureState.setDistortGUI(false);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                ForgeHooksClient.drawScreen(mc.currentScreen, mouseX, mouseY, mc.timer.renderPartialTicks);
            }

            mc.framebuffer = before;
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        // don't render the hand in every frame
        if (CaptureState.isCapturing() && CaptureState.getOrientation() != Orientation.FRONT) event.setCanceled(true);
    }

    public enum Orientation {
        FRONT, BACK, LEFT, RIGHT, BOTTOM, TOP;
    }

    @Override
    public EquirectangularFrameCapturer getThis() {
        return this;
    }

}
