package com.replaymod.panostream.mixin.optifine;

import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.panostream.capture.equi.CaptureState.tessellateRegion;

@Mixin(value = VboRenderList.class, priority = 900)
public abstract class OFMixinVboRenderList extends ChunkRenderContainer {
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "renderChunkLayer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/VboRenderList;drawRegion(IILnet/optifine/render/VboRegion;)V",
            shift = At.Shift.BEFORE,
            remap = false
    ))
    private void beforeDrawRegion(BlockRenderLayer layer, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer == null) return;
        capturer.setTessellationActive(tessellateRegion);
        tessellateRegion = false;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "renderChunkLayer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/VboRenderList;drawRegion(IILnet/optifine/render/VboRegion;)V",
            shift = At.Shift.AFTER,
            remap = false
    ))
    private void afterDrawRegion(BlockRenderLayer layer, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer == null) return;
        capturer.enableTessellation();
    }
}
