package com.replaymod.panostream.stream;

import com.google.common.base.Preconditions;
import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.PanoramicFrame;
import com.replaymod.panostream.capture.PanoramicFrameCapturer;
import com.replaymod.panostream.utils.FrameSizeUtil;
import com.replaymod.panostream.utils.StreamPipe;
import lombok.Getter;
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

    private boolean stopWriting = false;

    private AtomicBoolean active = new AtomicBoolean(false);

    @Getter
    private long finishTime;

    @Getter
    private State state = State.DISABLED;

    @Getter
    private int reconnectionAttempts;

    @Getter
    private int framesWritten;

    private PanoramicFrameCapturer panoramicFrameCapturer;

    private ConcurrentLinkedQueue<ByteBuffer> frameQueue;

    public synchronized boolean isActive() {
        return active.get();
    }

    public boolean isStopping() {
        return stopWriting && isActive();
    }

    public void streamToFFmpeg(VideoStreamer videoStreamer, List<String> command) {
        Preconditions.checkState(!active.get());
        stopWriting = false;

        active.set(true);

        new Thread(() -> {
            //starting the PanoramicFrameCapturer
            panoramicFrameCapturer = new PanoramicFrameCapturer(
                    FrameSizeUtil.singleFrameSize(PanoStreamMod.instance.getPanoStreamSettings().videoWidth.getIntValue()),
                    videoStreamer.getFps(), videoStreamer).register();

            boolean result = false;
            reconnectionAttempts = 0;

            while(!result && reconnectionAttempts++ < MAX_RECONNECTION_ATTEMPTS) {
                if(result = startFFmpeg(command)) {
                    panoramicFrameCapturer.setActive(true);
                    result = streamVideo();
                    panoramicFrameCapturer.setActive(false);
                    stopFFmpeg();
                }

                if(!result) {
                    state = State.RECONNECTING;
                }
            }

            state = result ? State.DISABLED : State.FAILED;
            finishTime = System.currentTimeMillis();

            active.set(false);
        }).start();

    }

    public void offerFrame(PanoramicFrame frame) {
        Preconditions.checkState(active.get());
        Preconditions.checkState(!stopWriting);

        //if the frameQueue is too full, we remove one element before adding our new element
        if(frameQueue.size() > MAX_FRAME_BUFFER) {
            frameQueue.poll();
        }

        ByteBuffer buf = frame.getByteBuffer();
        frameQueue.offer(buf);
    }

    public void stopStreaming() {
        Preconditions.checkState(active.get());
        Preconditions.checkState(!stopWriting);
        stopWriting = true;
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
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean streamVideo() {
        while(!stopWriting || !frameQueue.isEmpty()) {
            try {
                while(frameQueue.isEmpty()) {
                    Thread.sleep(10L);
                }

                channel.write(frameQueue.poll());

                // if two frames have been successfully written,
                // the pipe hasn't been closed after writing the first frame
                if(framesWritten++ > 1) {
                    state = State.STREAMING;
                    reconnectionAttempts = 0;
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void stopFFmpeg() {
        try {
            channel.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        //gently end the FFMPEG process
        long startTime = System.nanoTime();
        long rem = TimeUnit.SECONDS.toNanos(30);
        do {
            try {
                ffmpegProcess.exitValue();
                break;
            } catch(IllegalThreadStateException ex) {
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
