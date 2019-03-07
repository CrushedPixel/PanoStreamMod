package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    // Our geometry shader only supports GL_QUADS, not GL_LINE_STRIP. See MixinGlStateManager.
    @Inject(method = "drawBoundingBox(DDDDDDFFFF)V", at = @At("HEAD"), cancellable = true)
    private static void drawBoundingBoxAsQuads(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha, CallbackInfo ci) {
        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        if (capturer != null) {
            capturer.forceLazyRenderState();
        }
        if (!CaptureState.isGeometryShader() && !CaptureState.isTessEvalShader()) {
            return;
        }

        ci.cancel();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bb = tessellator.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        drawEdge(bb, red, green, blue, alpha, minX, minY, minZ, maxX, minY, minZ, 0, 1, 1);
        drawEdge(bb, red, green, blue, alpha, minX, minY, minZ, minX, maxY, minZ, 1, 0, 1);
        drawEdge(bb, red, green, blue, alpha, minX, minY, minZ, minX, minY, maxZ, 1, 1, 0);

        drawEdge(bb, red, green, blue, alpha, maxX, maxY, minZ, minX, maxY, minZ, 0, -1, 1);
        drawEdge(bb, red, green, blue, alpha, maxX, maxY, minZ, maxX, minY, minZ, -1, 0, 1);
        drawEdge(bb, red, green, blue, alpha, maxX, maxY, minZ, maxX, maxY, maxZ, -1, -1, 0);

        drawEdge(bb, red, green, blue, alpha, minX, maxY, maxZ, maxX, maxY, maxZ, 0, -1, -1);
        drawEdge(bb, red, green, blue, alpha, minX, maxY, maxZ, minX, minY, maxZ, 1, 0, -1);
        drawEdge(bb, red, green, blue, alpha, minX, maxY, maxZ, minX, maxY, minZ, 1, -1, 0);

        drawEdge(bb, red, green, blue, alpha, maxX, minY, maxZ, minX, minY, maxZ, 0, 1, -1);
        drawEdge(bb, red, green, blue, alpha, maxX, minY, maxZ, maxX, maxY, maxZ, -1, 0, -1);
        drawEdge(bb, red, green, blue, alpha, maxX, minY, maxZ, maxX, minY, minZ, -1, 1, 0);

        GlStateManager.disableCull(); // because both sides are visible for non-full blocks
        tessellator.draw();
        GlStateManager.enableCull();
    }

    private static void drawEdge(BufferBuilder bb, float red, float green, float blue, float alpha, double x0, double y0, double z0, double x1, double y1, double z1, int xd, int yd, int zd) {
        final double WIDTH = 0.02;
        if (xd != 0) {
            bb.pos(x0, y0, z0).color(red, green, blue, alpha).endVertex();
            bb.pos(x0 + xd * WIDTH, y0, z0).color(red, green, blue, alpha).endVertex();
            bb.pos(x1 + xd * WIDTH, y1, z1).color(red, green, blue, alpha).endVertex();
            bb.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        }
        if (yd != 0) {
            bb.pos(x0, y0, z0).color(red, green, blue, alpha).endVertex();
            bb.pos(x0, y0 + yd * WIDTH, z0).color(red, green, blue, alpha).endVertex();
            bb.pos(x1, y1 + yd * WIDTH, z1).color(red, green, blue, alpha).endVertex();
            bb.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        }
        if (zd != 0) {
            bb.pos(x0, y0, z0).color(red, green, blue, alpha).endVertex();
            bb.pos(x0, y0, z0 + zd * WIDTH).color(red, green, blue, alpha).endVertex();
            bb.pos(x1, y1, z1 + zd * WIDTH).color(red, green, blue, alpha).endVertex();
            bb.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        }
    }
}
