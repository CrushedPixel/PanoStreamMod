package com.replaymod.panostream.capture;

import lombok.Getter;
import lombok.Setter;

public class CaptureState {

    @Setter
    @Getter
    private static boolean capturing = false;

    @Setter
    @Getter
    public static PanoramicFrameCapturer.Orientation orientation = null;

    @Setter
    @Getter
    public static boolean distortGUI = false;

}
