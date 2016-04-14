package com.replaymod.panostream.stream;

import com.google.common.base.Preconditions;
import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.capture.PanoramicFrame;
import com.replaymod.panostream.capture.PanoramicFrameCapturer;
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
    private PanoramicFrameCapturer panoramicFrameCapturer;

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
        Preconditions.checkState(settings.videoWidth.getIntValue() == settings.videoHeight.getIntValue() * 2,
                "Output video's aspect ratio has to be 2:1, is %sx%s",
                settings.videoWidth.getIntValue(), settings.videoHeight.getIntValue());

        String commandArgs = settings.ffmpegArgs.getStringValue()
                .replace("%WIDTH%", String.valueOf(settings.videoWidth.getIntValue()))
                .replace("%HEIGHT%", String.valueOf(settings.videoHeight.getIntValue()))
                .replace("%FPS%", String.valueOf(this.fps = settings.fps.getIntValue()))
                .replace("%DESTINATION%", settings.rtmpServer.getStringValue());

        List<String> command = new ArrayList<>();

        String ffmpegCommand = PanoStreamMod.instance.getPanoStreamSettings().ffmpegCommand.getStringValue();

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
