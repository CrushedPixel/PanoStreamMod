package com.replaymod.panostream.capture.vr180;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

public class VR180ScreenshotCapturer extends VR180FrameCapturer {

    public VR180ScreenshotCapturer() {
        super(1080, 0, null);
    }

    private boolean requested = false;

    private final Object lock = new Object();

    @Override
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!requested) return;
        if (event.phase != TickEvent.Phase.END) return;

        super.onRenderTick(event);

        if (doCapture(false) != null) {
            requested = false;

            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    protected Framebuffer captureScreenshot() {
        synchronized(lock) {
            requested = true;

            try {
                lock.wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        return getComposedFramebuffer();
    }

    public void captureScreenshotAsync(File gameDirectory) {
        new Thread(() -> {
            final Framebuffer fb = captureScreenshot();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ITextComponent component = ScreenShotHelper.saveScreenshot(gameDirectory, 4 * 1080, 2 * 1080, fb);
                if(Minecraft.getMinecraft().player != null) Minecraft.getMinecraft().player.sendMessage(component);
            });
        }).start();
    }
}
