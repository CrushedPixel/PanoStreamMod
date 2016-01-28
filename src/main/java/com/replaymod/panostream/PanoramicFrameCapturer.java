package com.replaymod.panostream;

import com.replaymod.panostream.gui.EmptyGuiScreen;
import com.replaymod.panostream.utils.ScaledResolutionUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;


public class PanoramicFrameCapturer {

    private final Minecraft mc = Minecraft.getMinecraft();
    private PanoramicFrame panoramicFrame;

    @Getter @Setter
    private boolean active;

    @Getter
    private Orientation orientation;

    private final EmptyGuiScreen emptyGuiScreen = new EmptyGuiScreen();

    public PanoramicFrameCapturer(int frameSize) {
        panoramicFrame = new PanoramicFrame(frameSize);
        ScaledResolutionUtil.setWorldAndResolution(emptyGuiScreen, frameSize, frameSize);
    }

    public PanoramicFrameCapturer register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        return this;
    }

    public void setFrameSize(int frameSize) {
        panoramicFrame.setFrameSize(frameSize);
        ScaledResolutionUtil.setWorldAndResolution(emptyGuiScreen, frameSize, frameSize);
    }

    @SubscribeEvent
    public void capturePanoramicFrame(TickEvent.RenderTickEvent event) {
        if(mc.theWorld == null) return;
        if(!active) return;
        if(event.phase != TickEvent.Phase.END) return;

        //when rendering is finished, we render the six perspectives
        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;

        mc.displayWidth = mc.displayHeight = panoramicFrame.getFrameSize();

        ScaledResolutionUtil.setWorldAndResolution(mc.currentScreen);

        for(int i=0; i<6; i++) {
            orientation = Orientation.values()[i];

            GlStateManager.pushMatrix();
            panoramicFrame.bindFramebuffer(i);

            panoramicFrame.getFramebuffer(i).bindFramebufferTexture();

            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.enableTexture2D();

            renderWorld();
            renderOverlays();

            panoramicFrame.unbindFramebuffer();
            GlStateManager.popMatrix();
        }
        GlStateManager.pushMatrix();

        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableTexture2D();

        panoramicFrame.composeEquirectangular();

        GlStateManager.popMatrix();

        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        ScaledResolutionUtil.setWorldAndResolution(mc.currentScreen);

        mc.getFramebuffer().bindFramebuffer(true);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        orientation = null; //disable the orientation so MC renders the frame normally next time
    }

    private void renderWorld() {
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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        if(orientation != Orientation.FRONT) {
            if(mc.currentScreen != null) {
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                mc.entityRenderer.setupOverlayRendering();
                ForgeHooksClient.drawScreen(emptyGuiScreen, 0, 0, 0);
            }
        } else {
            this.mc.ingameGUI.renderGameOverlay(mc.timer.renderPartialTicks);
            if(mc.currentScreen != null) {
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

    public enum Orientation {
        FRONT, BACK, LEFT, RIGHT, BOTTOM, TOP;
    }

}
