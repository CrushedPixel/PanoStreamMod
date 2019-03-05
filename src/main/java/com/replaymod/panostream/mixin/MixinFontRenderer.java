package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {
    @Shadow
    @Final
    protected int[] charWidth;
    @Shadow
    @Final
    protected byte[] glyphWidth;
    @Shadow
    @Final
    protected ResourceLocation locationFontTexture;
    @Shadow
    protected float posX;
    @Shadow
    protected float posY;

    @Shadow
    protected abstract void bindTexture(ResourceLocation location);

    @Shadow
    private void loadGlyphTexture(int page) {}

    /**
     * @reason Our geometry shader only supports GL_QUADS via GL_LINES_ADJACENCY. See MixinGlStateManager.
     * @author johni0702
     */
    @Overwrite
    protected float renderDefaultChar(int ch, boolean italic) {
        int i = ch % 16 * 8;
        int j = ch / 16 * 8;
        int k = italic ? 1 : 0;
        bindTexture(this.locationFontTexture);
        int l = this.charWidth[ch];
        float f = (float)l - 0.01F;

        VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
        boolean wasTessellating = false;
        if (capturer != null) {
            if (GuiDebug.instance.skipFonts) return l;
            wasTessellating = capturer.isTessellationActive();
            capturer.setTessellationActive(GuiDebug.instance.tessellateFonts);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bb = tessellator.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bb.pos(this.posX + f - 1.0F - (float)k, this.posY + 7.99F, 0.0F);
        bb.tex(((float)i + f - 1.0F) / 128.0F, ((float)j + 7.99F) / 128.0F);
        bb.endVertex();
        bb.pos(this.posX + f - 1.0F + (float)k, this.posY, 0.0F);
        bb.tex(((float)i + f - 1.0F) / 128.0F, (float)j / 128.0F);
        bb.endVertex();
        bb.pos(this.posX + (float)k, this.posY, 0.0F);
        bb.tex((float)i / 128.0F, (float)j / 128.0F);
        bb.endVertex();
        bb.pos(this.posX - (float)k, this.posY + 7.99F, 0.0F);
        bb.tex((float)i / 128.0F, ((float)j + 7.99F) / 128.0F);
        bb.endVertex();
        tessellator.draw();

        if (capturer != null) capturer.setTessellationActive(wasTessellating);

        return (float)l;
    }

    /**
     * @reason Our geometry shader only supports GL_QUADS via GL_LINES_ADJACENCY. See MixinGlStateManager.
     * @author johni0702
     */
    @Overwrite
    protected float renderUnicodeChar(char ch, boolean italic)
    {
        int i = this.glyphWidth[ch] & 255;

        if (i == 0)
        {
            return 0.0F;
        }
        else
        {
            int j = ch / 256;
            this.loadGlyphTexture(j);
            int k = i >>> 4;
            int l = i & 15;
            float f = (float)k;
            float f1 = (float)(l + 1);
            float f2 = (float)(ch % 16 * 16) + f;
            float f3 = (float)((ch & 255) / 16 * 16);
            float f4 = f1 - f - 0.02F;
            float f5 = italic ? 1.0F : 0.0F;
            float ret = (f1 - f) / 2.0F + 1.0F;

            VR180FrameCapturer capturer = VR180FrameCapturer.getActive();
            boolean wasTessellating = false;
            if (capturer != null) {
                if (GuiDebug.instance.skipFonts) return ret;
                wasTessellating = capturer.isTessellationActive();
                capturer.setTessellationActive(GuiDebug.instance.tessellateFonts);
            }

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bb = tessellator.getBuffer();
            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            bb.pos(this.posX + f5, this.posY, 0.0F);
            bb.tex(f2 / 256.0F, f3 / 256.0F);
            bb.endVertex();
            bb.pos(this.posX - f5, this.posY + 7.99F, 0.0F);
            bb.tex(f2 / 256.0F, (f3 + 15.98F) / 256.0F);
            bb.endVertex();
            bb.pos(this.posX + f4 / 2.0F - f5, this.posY + 7.99F, 0.0F);
            bb.tex((f2 + f4) / 256.0F, (f3 + 15.98F) / 256.0F);
            bb.endVertex();
            bb.pos(this.posX + f4 / 2.0F + f5, this.posY, 0.0F);
            bb.tex((f2 + f4) / 256.0F, f3 / 256.0F);
            bb.endVertex();
            tessellator.draw();

            if (capturer != null) capturer.setTessellationActive(wasTessellating);

            return ret;
        }
    }
}
