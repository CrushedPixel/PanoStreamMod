package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Geometry shader do not support GL_QUADS, so we use GL_LINES_ADJACENCY instead and transform those into two
 * triangles in the geometry shader.
 * The ModelRenderer uses display lists so just swapping out the glDrawArrays call is insufficient in its case. Instead
 * we must maintain two display lists and swap them out as needed.
 *
 * @author johni0702
 */
@Mixin(ModelRenderer.class)
public class MixinModelRenderer {
    @Shadow
    private boolean compiled;
    @Shadow
    private int displayList;

    private boolean geomActive;
    private boolean otherCompiled;
    private int otherDisplayList;

    private void swapDisplayList() {
        int tmpList = otherDisplayList;
        otherDisplayList = displayList;
        displayList = tmpList;

        boolean tmpComp = otherCompiled;
        otherCompiled = compiled;
        compiled = tmpComp;

        geomActive = !geomActive;
    }

    private void selectDisplayList() {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (CaptureState.isGeometryShader() ^ geomActive) {
            swapDisplayList();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void selectDisplayList$0(float scale, CallbackInfo ci) {
        selectDisplayList();
    }

    @Inject(method = "renderWithRotation", at = @At("HEAD"))
    private void selectDisplayList$1(float scale, CallbackInfo ci) {
        selectDisplayList();
    }

    @Inject(method = "postRender", at = @At("HEAD"))
    private void selectDisplayList$2(float scale, CallbackInfo ci) {
        selectDisplayList();
    }
}
