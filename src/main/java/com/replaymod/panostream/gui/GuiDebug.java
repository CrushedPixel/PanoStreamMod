package com.replaymod.panostream.gui;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.utils.GlTimingQueries;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Consumer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public class GuiDebug extends AbstractGuiOverlay<GuiDebug> implements Typeable {
    { instance = this; }
    public static GuiDebug instance;

    public boolean alwaysTessellateChunks = false;
    public boolean neverTessellateChunk = false;
    public boolean tessellateGui = true;
    public boolean tessellateFonts = false;
    public boolean renderWorld = true;
    public boolean renderGui = true;
    public boolean compose = true;
    public boolean transfer = true;

    public GlTimingQueries queryWorldLeft = new GlTimingQueries(v -> nanoWorldLeftCpu = v, v -> nanoWorldLeftGpu = v);
    public GlTimingQueries queryWorldRight = new GlTimingQueries(v -> nanoWorldRightCpu = v, v -> nanoWorldRightGpu = v);
    public GlTimingQueries queryGuiLeft = new GlTimingQueries(v -> nanoGuiLeftCpu = v, v -> nanoGuiLeftGpu = v);
    public GlTimingQueries queryGuiRight = new GlTimingQueries(v -> nanoGuiRightCpu = v, v -> nanoGuiRightGpu = v);
    public GlTimingQueries queryCompose = new GlTimingQueries(v -> nanoComposeCpu = v, v -> nanoComposeGpu = v);
    public GlTimingQueries queryTransfer = new GlTimingQueries(v -> nanoTransferCpu = v, v -> nanoTransferGpu = v);

    private int nanoWorldLeftCpu, nanoWorldLeftGpu;
    private int nanoWorldRightCpu, nanoWorldRightGpu;
    private int nanoGuiLeftCpu, nanoGuiLeftGpu;
    private int nanoGuiRightCpu, nanoGuiRightGpu;
    private int nanoComposeCpu, nanoComposeGpu;
    private int nanoTransferCpu, nanoTransferGpu;

    private GuiLabel renderTimeWorldCpu = new GuiLabel(), renderTimeWorldGpu = new GuiLabel();
    private GuiLabel renderTimeGuiCpu = new GuiLabel(), renderTimeGuiGpu = new GuiLabel();
    private GuiLabel renderTimeLeftCpu = new GuiLabel(), renderTimeLeftGpu = new GuiLabel();
    private GuiLabel renderTimeRightCpu = new GuiLabel(), renderTimeRightGpu = new GuiLabel();
    private GuiLabel renderTimeBothCpu = new GuiLabel(), renderTimeBothGpu = new GuiLabel();
    private GuiLabel composeTimeCpu = new GuiLabel(), composeTimeGpu = new GuiLabel();
    private GuiLabel transferTimeCpu = new GuiLabel(), transferTimeGpu = new GuiLabel();
    private GuiPanel timingPanel = new GuiPanel(this)
            .setLayout(new GridLayout().setCellsEqualSize(true).setColumns(3).setSpacingX(5))
            .addElements(new GridLayout.Data(1, 0),
                    new GuiLabel(), new GuiLabel().setText("CPU Time (ns)"), new GuiLabel().setText("GPU Time (ns)"),
                    new GuiLabel().setText("Render world:"), renderTimeWorldCpu, renderTimeWorldGpu,
                    new GuiLabel().setText("Render gui:"), renderTimeGuiCpu, renderTimeGuiGpu,
                    new GuiLabel().setText("Render left:"), renderTimeLeftCpu, renderTimeLeftGpu,
                    new GuiLabel().setText("Render right:"), renderTimeRightCpu, renderTimeRightGpu,
                    new GuiLabel().setText("Render both:"), renderTimeBothCpu, renderTimeBothGpu,
                    new GuiLabel().setText("Compose:"), composeTimeCpu, composeTimeGpu,
                    new GuiLabel().setText("Transfer:"), transferTimeCpu, transferTimeGpu
            );

    private GuiPanel configPanel = new GuiPanel(this)
        .setLayout(new VerticalLayout())
            .addElements(null,
                    new ConfigCheckbox("Always tessellate chunks", alwaysTessellateChunks, v -> alwaysTessellateChunks = v),
                    new ConfigCheckbox("Never tessellate chunks", neverTessellateChunk, v -> neverTessellateChunk = v),
                    new ConfigCheckbox("Tessellate gui", tessellateGui, v -> tessellateGui = v),
                    new ConfigCheckbox("Tessellate fonts", tessellateFonts, v -> tessellateFonts = v),
                    new ConfigCheckbox("Render world", renderWorld, v -> renderWorld = v),
                    new ConfigCheckbox("Render gui", renderGui, v -> renderGui = v),
                    new ConfigCheckbox("Compose frames into one", compose, v -> compose = v),
                    new ConfigCheckbox("Download final frame", transfer, v -> transfer = v)
            );

    {
        setLayout(new CustomLayout<GuiDebug>() {
            @Override
            protected void layout(GuiDebug container, int width, int height) {
                if (isMouseVisible()) {
                    x(configPanel, width - 10 - width(configPanel));
                    y(configPanel, 26);
                } else {
                    x(configPanel, width);
                    y(configPanel, height);
                }
                x(timingPanel, width - 10 - width(timingPanel));
                y(timingPanel, height - 10 - height(timingPanel));
            }
        });
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (CaptureState.isCapturing()) return;
        renderTimeWorldCpu.setText(String.format("%,d", nanoWorldLeftCpu + nanoWorldRightCpu));
        renderTimeGuiCpu.setText(String.format("%,d", nanoGuiLeftCpu + nanoGuiRightCpu));
        renderTimeLeftCpu.setText(String.format("%,d", nanoWorldLeftCpu + nanoGuiLeftCpu));
        renderTimeRightCpu.setText(String.format("%,d", nanoWorldRightCpu + nanoGuiRightCpu));
        renderTimeBothCpu.setText(String.format("%,d", nanoWorldLeftCpu + nanoGuiLeftCpu + nanoWorldRightCpu + nanoGuiRightCpu));
        composeTimeCpu.setText(String.format("%,d", nanoComposeCpu));
        transferTimeCpu.setText(String.format("%,d", nanoTransferCpu));

        renderTimeWorldGpu.setText(String.format("%,d", nanoWorldLeftGpu + nanoWorldRightGpu));
        renderTimeGuiGpu.setText(String.format("%,d", nanoGuiLeftGpu + nanoGuiRightGpu));
        renderTimeLeftGpu.setText(String.format("%,d", nanoWorldLeftGpu + nanoGuiLeftGpu));
        renderTimeRightGpu.setText(String.format("%,d", nanoWorldRightGpu + nanoGuiRightGpu));
        renderTimeBothGpu.setText(String.format("%,d", nanoWorldLeftGpu + nanoGuiLeftGpu + nanoWorldRightGpu + nanoGuiRightGpu));
        composeTimeGpu.setText(String.format("%,d", nanoComposeGpu));
        transferTimeGpu.setText(String.format("%,d", nanoTransferGpu));
        super.draw(renderer, size, renderInfo);
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            setMouseVisible(false);
            return true;
        }
        return false;
    }

    @Override
    protected GuiDebug getThis() {
        return this;
    }

    private static class ConfigCheckbox extends GuiCheckbox {
        ConfigCheckbox(String label, boolean initialValue, Consumer<Boolean> consumer) {
            setLabel(label);
            setChecked(initialValue);
            onClick(() -> consumer.consume(isChecked()));
        }
    }
}
