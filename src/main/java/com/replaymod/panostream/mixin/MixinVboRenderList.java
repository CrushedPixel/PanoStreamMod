package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VboRenderList.class)
public abstract class MixinVboRenderList extends ChunkRenderContainer {
    private double viewEntityX;
    private double viewEntityY;
    private double viewEntityZ;

    private RenderChunk renderChunk;

    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
        viewEntityX = viewEntityXIn;
        viewEntityY = viewEntityYIn;
        viewEntityZ = viewEntityZIn;
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/VboRenderList;preRenderChunk(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V"))
    private void trackCurrentRenderChunk(VboRenderList vboRenderList, RenderChunk renderChunkIn) {
        renderChunk = renderChunkIn;
        vboRenderList.preRenderChunk(renderChunkIn);
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V"))
    private void drawArraysVR180(VertexBuffer vertexBuffer, int mode) {
        assert renderChunk != null;

        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer == null) {
            vertexBuffer.drawArrays(mode);
            return;
        }

        Vec3d center = renderChunk.boundingBox.getCenter();
        double d2 = center.squareDistanceTo(viewEntityX, viewEntityY, viewEntityZ);
        boolean tessellate = d2 <= 16 * 16;

        if (tessellate) {
            capturer.enableTessellation();
        } else {
            capturer.disableTessellation();
        }

        vertexBuffer.drawArrays(mode);

        renderChunk = null;
    }
}
