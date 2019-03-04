package com.replaymod.panostream.stream;

import com.google.common.base.Preconditions;
import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.FrameCapturer;
import com.replaymod.panostream.capture.equi.EquirectangularFrameCapturer;
import com.replaymod.panostream.utils.ByteBufferPool;
import com.replaymod.panostream.utils.FrameSizeUtil;
import com.replaymod.panostream.utils.StreamPipe;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingThread {

    private static final int MAX_FRAME_BUFFER = 5;
    public static final int MAX_RECONNECTION_ATTEMPTS = 10;

    private Process ffmpegProcess;

    private WritableByteChannel channel;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

    private AtomicBoolean stopWriting = new AtomicBoolean(false);

    private AtomicBoolean active = new AtomicBoolean(false);

    @Getter
    private long finishTime;

    @Getter
    private State state = State.DISABLED;

    @Getter
    private int reconnectionAttempts;

    @Getter
    private int framesWritten;

    private FrameCapturer frameCapturer;

    private ConcurrentLinkedQueue<ByteBuffer> frameQueue;

    public synchronized boolean isActive() {
        return active.get();
    }

    public boolean isStopping() {
        return stopWriting.get() && isActive();
    }

    public void streamToFFmpeg(VideoStreamer videoStreamer, List<String> command) {
        Preconditions.checkState(!active.get());
        stopWriting.set(false);

        active.set(true);

        Minecraft.getMinecraft().addScheduledTask(() -> {
            // destroy the old panoramicFrameCapturer if existent
            if (frameCapturer != null) {
                frameCapturer.destroy();
            }

            // TODO: VR180
            frameCapturer = new EquirectangularFrameCapturer(
                    FrameSizeUtil.singleFrameSize(PanoStreamMod.instance.getPanoStreamSettings().videoWidth.getValue()),
                    videoStreamer.getFps(), videoStreamer).register();

            new Thread(() -> {
                // start the EquirectangularFrameCapturer
                boolean result = false;
                reconnectionAttempts = 0;

                while (!result && reconnectionAttempts++ < MAX_RECONNECTION_ATTEMPTS) {
                    if (result = startFFmpeg(command)) {
                        frameCapturer.setActive(true);
                        result = streamVideo();
                        frameCapturer.setActive(false);
                        stopFFmpeg();
                    }

                    if (!result) {
                        state = State.RECONNECTING;
                    }
                }

                state = result ? State.DISABLED : State.FAILED;
                finishTime = System.currentTimeMillis();

                active.set(false);
            }).start();
        });
    }

    public void offerFrame(ByteBuffer buf) {
        Preconditions.checkState(active.get());
        Preconditions.checkState(!stopWriting.get());

        //if the frameQueue is too full, we remove one element before adding our new element
        if (frameQueue.size() > MAX_FRAME_BUFFER) {
            ByteBufferPool.release(frameQueue.poll());
        }

        frameQueue.offer(buf);
    }

    public void stopStreaming() {
        Preconditions.checkState(active.get());
        Preconditions.checkState(!stopWriting.get());
        stopWriting.set(true);
    }

    private boolean startFFmpeg(List<String> command) {
        try {
            framesWritten = 0;

            ffmpegProcess = new ProcessBuilder(command).start();

            OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream("streaming.log"), ffmpegLog);
            new StreamPipe(ffmpegProcess.getInputStream(), exportLogOut).start();
            new StreamPipe(ffmpegProcess.getErrorStream(), exportLogOut).start();

            channel = Channels.newChannel(ffmpegProcess.getOutputStream());

            frameQueue = new ConcurrentLinkedQueue<>();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean streamVideo() {
        while (true) {
            try {
                while (!stopWriting.get() && frameQueue.isEmpty()) {
                    Thread.sleep(10L);
                }

                if (stopWriting.get()) {
                    return true;
                }

                ByteBuffer buffer = frameQueue.poll();
                channel.write(buffer);
                ByteBufferPool.release(buffer);

                // if two frames have been successfully written,
                // the pipe hasn't been closed after writing the first frame
                if (framesWritten++ > 1) {
                    state = State.STREAMING;
                    reconnectionAttempts = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void stopFFmpeg() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //gently end the FFMPEG process
        long startTime = System.nanoTime();
        long rem = TimeUnit.SECONDS.toNanos(30);
        do {
            try {
                ffmpegProcess.exitValue();
                break;
            } catch (IllegalThreadStateException ex) {
                if (rem > 0) {
                    try {
                        Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            rem = TimeUnit.SECONDS.toNanos(30) - (System.nanoTime() - startTime);
        } while (rem > 0);

        ffmpegProcess.destroy();
    }

    public enum State {
        STREAMING, RECONNECTING, FAILED, DISABLED
    }

}
