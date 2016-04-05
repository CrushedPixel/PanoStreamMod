package com.replaymod.panostream.stream;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.PanoramicFrame;
import com.replaymod.panostream.utils.StreamPipe;
import com.replaymod.panostream.utils.StringUtil;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.lwjgl.util.ReadableDimension;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class VideoStreamer {

    private static final int MAX_FRAME_BUFFER = 5;

    private Process ffmpegProcess;

    private ReadableDimension frameSize;

    @Getter
    private int fps;

    private String destination;

    private OutputStream outputStream;

    private WritableByteChannel channel;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

    private ConcurrentLinkedQueue<ByteBuffer> frameQueue;

    @Getter
    private boolean active;

    public VideoStreamer(ReadableDimension frameSize, int fps, String destination) {
        this.frameSize = frameSize;
        this.fps = fps;
        this.destination = destination;
    }

    public void setFrameSize(ReadableDimension frameSize) {
        if(active) throw new IllegalStateException("Can't change Frame Size while VideoStreamer is active");
        this.frameSize = frameSize;
    }

    public void setFps(int fps) {
        if(active) throw new IllegalStateException("Can't change FPS while VideoStreamer is active");
        this.fps = fps;
    }

    public void setDestination(String destination) {
        if(active) throw new IllegalStateException("Can't change Destination while VideoStreamer is active");
        this.destination = destination;
    }

    public void startStream() throws IOException {
        String commandArgs = PanoStreamMod.instance.getPanoStreamSettings().ffmpegArgs.getStringValue()
                .replace("%WIDTH%", String.valueOf(frameSize.getWidth()))
                .replace("%HEIGHT%", String.valueOf(frameSize.getHeight()))
                .replace("%FPS%", String.valueOf(fps))
                .replace("%DESTINATION%", destination);

        List<String> command = new ArrayList<>();

        String ffmpegCommand = PanoStreamMod.instance.getPanoStreamSettings().ffmpegCommand.getStringValue();

        command.add(ffmpegCommand);
        command.addAll(StringUtil.translateCommandline(commandArgs));

        ffmpegProcess = new ProcessBuilder(command).start();

        System.out.println("Starting " + ffmpegCommand + " with args: " + commandArgs);

        OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream("streaming.log"), ffmpegLog);
        new StreamPipe(ffmpegProcess.getInputStream(), exportLogOut).start();
        new StreamPipe(ffmpegProcess.getErrorStream(), exportLogOut).start();

        outputStream = ffmpegProcess.getOutputStream();
        channel = Channels.newChannel(outputStream);

        frameQueue = new ConcurrentLinkedQueue<>();

        active = true;

        while(active || !frameQueue.isEmpty()) {
            try {
                while(frameQueue.isEmpty()) {
                    Thread.sleep(10L);
                }

                channel.write(frameQueue.poll());
            } catch(IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Finishing FFMPEG writing");

        IOUtils.closeQuietly(outputStream);

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

        System.out.println("FFMPEG writing finished");
    }

    public void stopStream() {
        active = false;
    }

    public synchronized void writeFrameToStream(PanoramicFrame frame) {
        if(!active) throw new IllegalStateException("VideoStreamer is currently not active");

        //if the frameQueue is too full, we remove one element before adding our new element
        if(frameQueue.size() > MAX_FRAME_BUFFER) {
            frameQueue.poll();
        }

        ByteBuffer buf = frame.getByteBuffer();
        frameQueue.offer(buf);
    }

}
