package com.replaymod.panostream.capture.equi;

import com.replaymod.panostream.PanoStreamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

public class EquirectangularScreenshotCapturer extends EquirectangularFrameCapturer {

    public EquirectangularScreenshotCapturer() {
        super(1080, 0, null);
    }

    private boolean requested = false;

    private final Object lock = new Object();

    @SubscribeEvent
    @Override
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if(!requested) return;
        if(event.phase != TickEvent.Phase.END) return;

        if (doCapture(false, PanoStreamMod.instance.getPanoStreamSettings().stabilizeOutput.getValue()) != null) {
            requested = false;

            synchronized(lock) {
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

        return equirectangularFrame.getComposedFramebuffer();
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
