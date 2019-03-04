package com.replaymod.panostream.stream;

import com.google.common.base.Preconditions;
import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.settings.PanoStreamSettings;
import com.replaymod.panostream.utils.StringUtil;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VideoStreamer {

    private static final Logger LOGGER = LogManager.getLogger();

    @Getter
    private int fps;

    @Getter
    private final StreamingThread streamingThread = new StreamingThread();

    public void toggleStream() {
        if (streamingThread.isStopping()) {
            LOGGER.warn("Stream is already stopping!");
            return;
        }

        try {
            if (!streamingThread.isActive()) startStream(PanoStreamMod.instance.getPanoStreamSettings());
            else stopStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startStream(PanoStreamSettings settings) throws IOException {
        if (settings.vr180.getValue()) {
            Preconditions.checkState(settings.videoWidth.getValue() * 2 == settings.videoHeight.getValue(),
                    "Output video's aspect ratio has to be 1:2, is %sx%s",
                    settings.videoWidth.getValue(), settings.videoHeight.getValue());
        } else {
            Preconditions.checkState(settings.videoWidth.getValue() == settings.videoHeight.getValue() * 2,
                    "Output video's aspect ratio has to be 2:1, is %sx%s",
                    settings.videoWidth.getValue(), settings.videoHeight.getValue());
        }

        LOGGER.info("Starting stream...");

        String commandArgs = settings.ffmpegArgs.getValue()
                .replace("%WIDTH%", String.valueOf(settings.videoWidth.getValue()))
                .replace("%HEIGHT%", String.valueOf(settings.videoHeight.getValue()))
                .replace("%FPS%", String.valueOf(this.fps = settings.fps.getValue()))
                .replace("%DESTINATION%", settings.rtmpServer.getValue());

        List<String> command = new ArrayList<>();

        String ffmpegCommand = PanoStreamMod.instance.getPanoStreamSettings().ffmpegCommand.getValue();

        command.add(ffmpegCommand);
        command.addAll(StringUtil.translateCommandline(commandArgs));

        streamingThread.streamToFFmpeg(this, settings.vr180.getValue(), command);
    }

    private void stopStream() {
        LOGGER.info("Stopping stream...");
        streamingThread.stopStreaming();
    }

    public synchronized void writeFrameToStream(ByteBuffer frame) {
        streamingThread.offerFrame(frame);
    }

}
