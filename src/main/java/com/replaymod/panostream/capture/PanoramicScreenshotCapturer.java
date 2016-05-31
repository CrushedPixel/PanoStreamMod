package com.replaymod.panostream.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

public class PanoramicScreenshotCapturer extends PanoramicFrameCapturer {

    public PanoramicScreenshotCapturer() {
        super(1080, 0, null);
    }

    private boolean requested = false;

    private final Object lock = new Object();

    @SubscribeEvent
    public void captureFrame(TickEvent.RenderTickEvent event) {
        if(!requested) return;
        if(panoramicFrame == null) return;
        if(event.phase != TickEvent.Phase.END) return;

        doCapture(false);

        requested = false;

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    public Framebuffer capturePanoScreenshot() {
        synchronized(lock) {
            requested = true;

            try {
                lock.wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        return getPanoramicFrame().getComposedFramebuffer();
    }

    public void captureScreenshotAsync(File gameDirectory) {
        new Thread(() -> {
            final Framebuffer fb = capturePanoScreenshot();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                IChatComponent component = ScreenShotHelper.saveScreenshot(gameDirectory, 4 * 1080, 2 * 1080, fb);
                Minecraft.getMinecraft().thePlayer.addChatComponentMessage(component);
            });
        }).start();
    }
}
