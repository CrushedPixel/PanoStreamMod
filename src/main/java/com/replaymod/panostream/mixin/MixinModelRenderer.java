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
 * Same for the tessellation shader (except with GL_PATCHES).
 *
 * @author johni0702
 */
@Mixin(ModelRenderer.class)
public class MixinModelRenderer {
    @Shadow
    private boolean compiled;
    @Shadow
    private int displayList;

    private boolean normalActive;
    private boolean normalCompiled;
    private int normalDisplayList;
    private boolean geomActive;
    private boolean geomCompiled;
    private int geomDisplayList;
    private boolean tessActive;
    private boolean tessCompiled;
    private int tessDisplayList;

    private void storeActive() {
        if (geomActive) {
            geomActive = false;
            geomCompiled = compiled;
            geomDisplayList = displayList;
        } else if (tessActive) {
            tessActive = false;
            tessCompiled = compiled;
            tessDisplayList = displayList;
        } else if (normalActive) {
            normalActive = false;
            normalCompiled = compiled;
            normalDisplayList = displayList;
        }
    }

    private void selectDisplayList() {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (CaptureState.isGeometryShader()) {
            if (!geomActive) {
                storeActive();
                geomActive = true;
                compiled = geomCompiled;
                displayList = geomDisplayList;
            }
        } else if (CaptureState.isTessEvalShader()) {
            if (!tessActive) {
                storeActive();
                tessActive = true;
                compiled = tessCompiled;
                displayList = tessDisplayList;
            }
        } else {
            if (!normalActive) {
                storeActive();
                normalActive = true;
                compiled = normalCompiled;
                displayList = normalDisplayList;
            }
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
