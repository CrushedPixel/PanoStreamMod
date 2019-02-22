package com.replaymod.panostream.capture.equi;

import lombok.Getter;
import lombok.Setter;

public class CaptureState {

    @Setter
    @Getter
    private static boolean capturing = false;

    @Setter
    @Getter
    public static EquirectangularFrameCapturer.Orientation orientation = null;

    @Setter
    @Getter
    public static boolean distortGUI = false;

}
