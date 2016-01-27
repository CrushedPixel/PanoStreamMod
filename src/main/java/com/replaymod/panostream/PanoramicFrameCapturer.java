package com.replaymod.panostream;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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

    public PanoramicFrameCapturer(int frameSize) {
        panoramicFrame = new PanoramicFrame(frameSize);
    }

    public void setFrameSize(int frameSize) {
        panoramicFrame.setFrameSize(frameSize);
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

        for(int i=0; i<6; i++) {
            orientation = Orientation.values()[i];

            GlStateManager.pushMatrix();
            panoramicFrame.bindFramebuffer(i);

            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.enableTexture2D();

            renderWorld();

            panoramicFrame.unbindFramebuffer();
            GlStateManager.popMatrix();
        }

        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        mc.getFramebuffer().bindFramebuffer(true);

        orientation = null; //disable the orientation so MC renders the frame normally next time
    }

    private void renderWorld() {
        //rendering the world with as little overweight function calls as possible
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorldPass(2, mc.timer.elapsedPartialTicks, 0);
    }

    public enum Orientation {
        FRONT, BACK, LEFT, RIGHT, BOTTOM, TOP;
    }

}
