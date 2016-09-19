package com.replaymod.panostream.stream;

import com.google.common.base.Preconditions;
import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.PanoramicFrame;
import com.replaymod.panostream.settings.PanoStreamSettings;
import com.replaymod.panostream.utils.StringUtil;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoStreamer {

    @Getter
    private int fps;

    @Getter
    private final StreamingThread streamingThread = new StreamingThread();

    public void toggleStream() {
        if(streamingThread.isStopping()) return;

        try {
            if(!streamingThread.isActive()) startStream(PanoStreamMod.instance.getPanoStreamSettings());
            else stopStream();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void startStream(PanoStreamSettings settings) throws IOException {
        Preconditions.checkState(settings.videoWidth.getValue() == settings.videoHeight.getValue() * 2,
                "Output video's aspect ratio has to be 2:1, is %sx%s",
                settings.videoWidth.getValue(), settings.videoHeight.getValue());

        String commandArgs = settings.ffmpegArgs.getValue()
                .replace("%WIDTH%", String.valueOf(settings.videoWidth.getValue()))
                .replace("%HEIGHT%", String.valueOf(settings.videoHeight.getValue()))
                .replace("%FPS%", String.valueOf(this.fps = settings.fps.getValue()))
                .replace("%DESTINATION%", settings.rtmpServer.getValue());

        List<String> command = new ArrayList<>();

        String ffmpegCommand = PanoStreamMod.instance.getPanoStreamSettings().ffmpegCommand.getValue();

        command.add(ffmpegCommand);
        command.addAll(StringUtil.translateCommandline(commandArgs));

        streamingThread.streamToFFmpeg(this, command);
    }

    private void stopStream() {
        streamingThread.stopStreaming();
    }

    public synchronized void writeFrameToStream(PanoramicFrame frame) {
        streamingThread.offerFrame(frame);
    }

}
