package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.replaymod.panostream.capture.equi.CaptureState.tessellateRegion;

@Mixin(VboRenderList.class)
public abstract class MixinVboRenderList extends ChunkRenderContainer {
    private double viewEntityX;
    private double viewEntityY;
    private double viewEntityZ;

    private RenderChunk renderChunk;

    // Optifine "Render Region" compatibility
    private boolean renderRegions; // whether it's enabled

    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
        viewEntityX = viewEntityXIn;
        viewEntityY = viewEntityYIn;
        viewEntityZ = viewEntityZIn;
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getVertexBufferByLayer(I)Lnet/minecraft/client/renderer/vertex/VertexBuffer;"))
    private VertexBuffer trackCurrentRenderChunk(RenderChunk renderChunk, int layer) {
        this.renderChunk = renderChunk;
        this.renderRegions = true; // assume true. if it isn't, then the method below will be called before drawArrays
        return renderChunk.getVertexBufferByLayer(layer);
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/VboRenderList;preRenderChunk(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V"))
    private void markAsNotRenderRegions(VboRenderList vboRenderList, RenderChunk renderChunkIn) {
        renderRegions = false;
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

        if (renderRegions) {
            if (!tessellateRegion && isInTessellationRange(renderChunk)) {
                tessellateRegion = true;
            }
            vertexBuffer.drawArrays(mode);
        } else {
            capturer.setTessellationActive(isInTessellationRange(renderChunk));

            vertexBuffer.drawArrays(mode);

            capturer.enableTessellation();
        }
        renderChunk = null;
    }

    private boolean isInTessellationRange(RenderChunk renderChunk) {
        Vec3d center = renderChunk.boundingBox.getCenter();
        double d2 = center.squareDistanceTo(viewEntityX, viewEntityY, viewEntityZ);
        boolean tessellate;
        if (GuiDebug.instance.tessellationShader) {
            // Tessellation shader is far cheaper, so we can me more wasteful on chunks which we run through it
            // and therefore make sure there are absolutely no discontinuities.
            tessellate = d2 <= 32 * 32;
        } else {
            tessellate = d2 <= 16 * 16;
        }

        if (GuiDebug.instance.alwaysTessellateChunks) {
            tessellate = true;
        }
        if (GuiDebug.instance.neverTessellateChunks) {
            tessellate = false;
        }

        return tessellate;
    }
}
