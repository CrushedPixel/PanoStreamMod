package com.replaymod.panostream.capture.equi;

import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import lombok.Getter;
import lombok.Setter;

public class CaptureState {

    @Setter
    @Getter
    private static boolean capturing = false;

    /**
     * Whether a geometry shader is currently active.
     * Beware that shader state is lazily managed and {@link VR180FrameCapturer#forceLazyRenderState()} needs to be
     * called beforehand to get proper results.
     */
    @Setter
    @Getter
    private static boolean geometryShader = false;

    /**
     * Whether a tessellation evaluation shader is currently active.
     * Beware that shader state is lazily managed and {@link VR180FrameCapturer#forceLazyRenderState()} needs to be
     * called beforehand to get proper results.
     */
    @Setter
    @Getter
    private static boolean tessEvalShader = false;

    @Setter
    @Getter
    public static EquirectangularFrameCapturer.Orientation orientation = null;

    @Setter
    @Getter
    public static boolean distortGUI = false;

    // Static, shared stated between MixinVboRenderList and OFMixinVboRenderList.
    // To be moved into the mixin once the mixins can be merged because optional Injects are supported.
    public static boolean tessellateRegion; // whether any chunk in the current region needs tessellation
}
