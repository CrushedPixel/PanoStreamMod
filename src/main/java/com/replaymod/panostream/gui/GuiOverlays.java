package com.replaymod.panostream.gui;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.stream.StreamingThread;
import com.replaymod.panostream.utils.GuiUtils;
import com.replaymod.panostream.utils.Registerable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GuiOverlays extends Registerable<GuiOverlays> {

    public static final ResourceLocation OVERLAY_RESOURCE = new ResourceLocation("panostream", "overlay.png");
    public static final int TEXTURE_SIZE = 64;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onOverlayRender(TickEvent.RenderTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        if(PanoStreamMod.instance.getVideoStreamer().getStreamingThread().getState()
                == StreamingThread.State.DISABLED) return;

        GlStateManager.pushAttrib();

        mc.renderEngine.bindTexture(OVERLAY_RESOURCE);
        GlStateManager.enableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1);

        int width = new ScaledResolution(mc).getScaledWidth();

        int x = width - 10 - 16;
        int y = 10;

        switch(PanoStreamMod.instance.getVideoStreamer().getStreamingThread().getState()) {
            case STREAMING:
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
                break;
            case RECONNECTING:
                int rotation = (int)(System.currentTimeMillis() % 1000) / 250 * 90;
                GuiUtils.drawRotatedRectWithCustomSizedTexture(x, y, rotation, 0, 48, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
                GuiUtils.drawCenteredString(String.format("%s/%s",
                        PanoStreamMod.instance.getVideoStreamer().getStreamingThread().getReconnectionAttempts(),
                        StreamingThread.MAX_RECONNECTION_ATTEMPTS), x + 8, y + 16 + 5, 0xffffff);
                break;
            case FAILED:
                long diff = System.currentTimeMillis() - PanoStreamMod.instance.getVideoStreamer()
                        .getStreamingThread().getFinishTime();

                //stays visible for 5 seconds, then disappears over 2 seconds
                float opacity = 1 - (Math.max(0, Math.min(1, (diff - 5000) / 2000f)));

                GlStateManager.pushAttrib();
                GlStateManager.color(1, 1, 1, opacity);
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
                GlStateManager.popAttrib();
                break;
        }

        GlStateManager.popAttrib();
    }

    public GuiOverlays getThis() {
        return this;
    }

}
