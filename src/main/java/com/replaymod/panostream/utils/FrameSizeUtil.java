package com.replaymod.panostream.utils;

import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

public class FrameSizeUtil {

    public static ReadableDimension composedFrameSize(int frameSize) {
        return new Dimension(frameSize * 4, frameSize * 2);
    }

}
