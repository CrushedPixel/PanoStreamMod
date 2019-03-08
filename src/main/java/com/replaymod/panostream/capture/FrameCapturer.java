package com.replaymod.panostream.capture;

import com.replaymod.panostream.stream.VideoStreamer;
import com.replaymod.panostream.utils.Registerable;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.nio.ByteBuffer;

public abstract class FrameCapturer extends Registerable<FrameCapturer> {

    @Getter
    @Setter
    private boolean active;

    private final int fps;

    private final VideoStreamer videoStreamer;

    private long lastCaptureTime = System.nanoTime();
    private boolean skipFrame = true;

    public FrameCapturer(int fps, VideoStreamer videoStreamer) {
        this.fps = fps;
        this.videoStreamer = videoStreamer;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (!active
                    || !videoStreamer.getStreamingThread().isActive()
                    || videoStreamer.getStreamingThread().isStopping()) {
                skipFrame = true;
                return;
            }

            // limit the framerate
            long curTime = System.nanoTime();
            skipFrame = curTime - lastCaptureTime < 1000 / 1000000 / fps;
            if (skipFrame) {
                return;
            }
            lastCaptureTime = curTime;
            beginFrame();
        }

        if (event.phase == TickEvent.Phase.END && !skipFrame) {
            // perform capturing
            ByteBuffer frameBuf = captureFrame();
            if (frameBuf != null) {
                videoStreamer.writeFrameToStream(frameBuf);
            }
        }
    }

    protected void beginFrame() {}

    protected abstract ByteBuffer captureFrame();

    public abstract void destroy();

    @Override
    public FrameCapturer getThis() {
        return this;
    }
}
