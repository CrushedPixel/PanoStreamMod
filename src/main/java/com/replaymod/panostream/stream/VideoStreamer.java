package com.replaymod.panostream.stream;

import com.replaymod.panostream.capture.PanoramicFrame;
import com.replaymod.panostream.utils.StreamPipe;
import com.replaymod.panostream.utils.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.lwjgl.util.ReadableDimension;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoStreamer {

    private Process ffmpegProcess;

    //the default ffmpeg arguments to output an FLV video to a stream or a file
    @Setter
    private String ffmpegCommand = "ffmpeg";

    @Setter
    private String ffmpegArguments = "-y -f rawvideo -pix_fmt rgb24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -an \"%DESTINATION%\"";

    private ReadableDimension frameSize;
    private int fps;
    private String destination;

    private OutputStream outputStream;
    private WritableByteChannel channel;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

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
        String commandArgs = ffmpegArguments.replace("%WIDTH%", String.valueOf(frameSize.getWidth()))
                .replace("%HEIGHT%", String.valueOf(frameSize.getHeight()))
                .replace("%FPS%", String.valueOf(fps))
                .replace("%DESTINATION%", destination);

        List<String> command = new ArrayList<>();

        command.add(ffmpegCommand);
        command.addAll(StringUtil.translateCommandline(commandArgs));

        ffmpegProcess = new ProcessBuilder(command).start();

        System.out.println("Starting " + ffmpegCommand + " with args: " + commandArgs);

        OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream("streaming.log"), ffmpegLog);
        new StreamPipe(ffmpegProcess.getInputStream(), exportLogOut).start();
        new StreamPipe(ffmpegProcess.getErrorStream(), exportLogOut).start();

        outputStream = ffmpegProcess.getOutputStream();
        channel = Channels.newChannel(outputStream);

        active = true;
    }

    public void stopStream() {
        active = false;

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
    }

    public void writeFrameToStream(PanoramicFrame frame) {
        if(!active) throw new IllegalStateException("VideoStreamer is currently not active");

        try {
            channel.write(frame.getByteBuffer());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
