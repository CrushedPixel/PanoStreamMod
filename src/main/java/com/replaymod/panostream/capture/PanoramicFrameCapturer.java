package com.replaymod.panostream.capture;

import com.replaymod.panostream.gui.EmptyGuiScreen;
import com.replaymod.panostream.stream.VideoStreamer;
import com.replaymod.panostream.utils.Registerable;
import com.replaymod.panostream.utils.ScaledResolutionUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;


public class PanoramicFrameCapturer extends Registerable<PanoramicFrameCapturer> {

    private final Minecraft mc = Minecraft.getMinecraft();
    private PanoramicFrame panoramicFrame;

    @Getter
    @Setter
    private boolean active;

    private final int fps;

    private VideoStreamer videoStreamer;

    @Getter
    private Orientation orientation;

    @Getter
    private boolean distortGUI = false;

    private final EmptyGuiScreen emptyGuiScreen = new EmptyGuiScreen();

    private long lastCaptureTime = System.currentTimeMillis();

    public PanoramicFrameCapturer(int frameSize, int fps, VideoStreamer videoStreamer) {
        this.videoStreamer = videoStreamer;
        this.fps = fps;

        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                panoramicFrame = new PanoramicFrame(frameSize);
                ScaledResolutionUtil.setWorldAndResolution(emptyGuiScreen, frameSize, frameSize);
            }
        });
    }

    @SubscribeEvent
    public void capturePanoramicFrame(TickEvent.RenderTickEvent event) {
        if(!active || panoramicFrame == null) return;
        if(mc.currentScreen instanceof GuiMainMenu) return; //TODO: handle GuiMainMenu
        if(event.phase != TickEvent.Phase.END) return;

        long curTime = System.currentTimeMillis();
        if(curTime - lastCaptureTime < 1000 / fps) return; //cap the framerate

        lastCaptureTime = curTime;

        //when rendering is finished, we render the six perspectives
        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;

        mc.displayWidth = mc.displayHeight = panoramicFrame.getFrameSize();

        for(int i=0; i<6; i++) {
            orientation = Orientation.values()[i];

            panoramicFrame.bindFramebuffer(i);

            renderWorld();
            renderOverlays();

            panoramicFrame.unbindFramebuffer();
        }

        panoramicFrame.composeEquirectangular();

        if(videoStreamer.getStreamingThread().isActive() && !videoStreamer.getStreamingThread().isStopping())
            videoStreamer.writeFrameToStream(panoramicFrame);

        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        orientation = null; //disable the orientation so MC renders the frame normally next time
    }

    private void renderWorld() {
        if(mc.theWorld == null) return;
        //rendering the world with as little overweight function calls as possible
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    private void renderOverlays() {
        if(this.mc.gameSettings.hideGUI && this.mc.currentScreen == null) return;

        //if a GuiScreen is opened, render the default overlay on the other sides
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        if(orientation != Orientation.FRONT) {
            if(mc.currentScreen != null) {
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                mc.entityRenderer.setupOverlayRendering();
                ForgeHooksClient.drawScreen(emptyGuiScreen, 0, 0, 0);
            }
        } else {
            if(mc.thePlayer != null) mc.ingameGUI.renderGameOverlay(mc.timer.renderPartialTicks);
            if(mc.currentScreen != null) {
                distortGUI = true;
                mc.entityRenderer.setupOverlayRendering(); //re-setup overlay rendering with distortion enabled
                distortGUI = false;
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                ForgeHooksClient.drawScreen(mc.currentScreen, 0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        //don't render the hand in every frame
        if(active && orientation != null && orientation != Orientation.FRONT) event.setCanceled(true);
    }

    /* Maybe we'll want to capture the GUI only on a separate Framebuffer later
    @SubscribeEvent
    public void onRenderGui(GuiScreenEvent.DrawScreenEvent event) {
        //render the Gui on the PanoramicFrameCapturer's Gui Framebuffer
        PanoramicFrameCapturer capturer = PanoStreamMod.instance.getPanoramicFrameCapturer();

        if(capturer.isActive() && capturer.getOrientation() == null) {
            Framebuffer framebuffer = capturer.getGuiFramebuffer();
            if(framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
                framebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
            } else {
                framebuffer.bindFramebuffer(true);
            }

            event.gui.drawScreen(event.mouseX, event.mouseY, event.renderPartialTicks);

            mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    */

    public enum Orientation {
        FRONT, BACK, LEFT, RIGHT, BOTTOM, TOP;
    }

    @Override
    public PanoramicFrameCapturer getThis() {
        return this;
    }

}
