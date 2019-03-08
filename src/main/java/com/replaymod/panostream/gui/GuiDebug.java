package com.replaymod.panostream.gui;

import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.capture.vr180.VR180FrameCapturer;
import com.replaymod.panostream.utils.GlTimingQueries;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiNumberField;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Consumer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

public class GuiDebug extends AbstractGuiOverlay<GuiDebug> implements Typeable {
    { instance = this; }
    public static GuiDebug instance;

    public boolean markLeftEye = false;
    public boolean wireframe = false;
    public boolean zeroPass = false;
    public boolean singlePass = true;
    public boolean tessellationShader = GLContext.getCapabilities().OpenGL40;
    public int maxTessLevel = 30;
    public boolean geometryShaderInstancing = GLContext.getCapabilities().OpenGL40;
    public boolean alwaysUseGeometryShaderInstancing = false;
    public boolean drawInstanced = false; // once fully working: GLContext.getCapabilities().OpenGL31;
    public boolean alwaysTessellateChunks = false;
    public boolean neverTessellateChunks = false;
    public boolean tessellateGui = true;
    public boolean tessellateFonts = false;
    public boolean skipFonts = false;
    public boolean renderWorld = true;
    public boolean renderGui = true;
    public boolean compose = true;
    public boolean transfer = true;
    public boolean useReadPixels = true;
    public int pbos = 3;

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
    public int nanoComposeCpu, nanoComposeGpu;
    private int nanoTransferCpu, nanoTransferGpu;
    public int glDrawArraysCounter;
    public int programSwitchesCounter;

    private GuiLabel renderTimeWorldCpu = new GuiLabel(), renderTimeWorldGpu = new GuiLabel();
    private GuiLabel renderTimeGuiCpu = new GuiLabel(), renderTimeGuiGpu = new GuiLabel();
    private GuiLabel renderTimeLeftCpu = new GuiLabel(), renderTimeLeftGpu = new GuiLabel();
    private GuiLabel renderTimeRightCpu = new GuiLabel(), renderTimeRightGpu = new GuiLabel();
    private GuiLabel renderTimeBothCpu = new GuiLabel(), renderTimeBothGpu = new GuiLabel();
    private GuiLabel composeTimeCpu = new GuiLabel(), composeTimeGpu = new GuiLabel();
    private GuiLabel transferTimeCpu = new GuiLabel(), transferTimeGpu = new GuiLabel();
    private GuiLabel glDrawArrays = new GuiLabel();
    private GuiLabel programSwitches = new GuiLabel();
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
                    new GuiLabel().setText("Transfer:"), transferTimeCpu, transferTimeGpu,
                    new GuiLabel().setText("glDrawArrays:"), glDrawArrays, new GuiLabel(),
                    new GuiLabel().setText("Program switches:"), programSwitches, new GuiLabel()
            );

    private GuiNumberField maxTessLevelField = new GuiNumberField().setSize(50, 20).setValidateOnFocusChange(true).setValue(maxTessLevel);
    {
        maxTessLevelField.onEnter(() -> {
            maxTessLevel = Math.max(1, maxTessLevelField.getInteger());
            maxTessLevelField.setValue(maxTessLevel);
            reloadFrameAndPrograms();
        });
    }
    private GuiNumberField pbosField = new GuiNumberField().setSize(50, 20).setValidateOnFocusChange(true).setValue(pbos);
    {
        pbosField.onEnter(() -> {
            pbos = Math.max(1, pbosField.getInteger());
            pbosField.setValue(pbos);
            VR180FrameCapturer capturer = VR180FrameCapturer.getCurrent();
            if (capturer != null) {
                capturer.recreateFrame();
            }
        });
    }
    private ConfigCheckbox zeroPassCheckbox;
    private ConfigCheckbox singlePassCheckbox;
    private ConfigCheckbox renderWorldCheckbox;
    private ConfigCheckbox renderGuiCheckbox;
    private ConfigCheckbox useReadPixelsCheckbox;
    private GuiPanel configPanel = new GuiPanel(this)
        .setLayout(new VerticalLayout())
            .addElements(null,
                    new ConfigCheckbox("Mark left eye", markLeftEye, v -> {
                        markLeftEye = v;
                        reloadFrameAndPrograms();
                    }),
                    new ConfigCheckbox("Wireframe", wireframe, v -> wireframe = v),
                    zeroPassCheckbox = new ConfigCheckbox("Zero Pass", zeroPass, v -> {
                        zeroPass = v;
                        if (v) {
                            singlePassCheckbox.setChecked(true).onClick();
                        }
                        renderWorld = renderWorldCheckbox.setEnabled(!v).isChecked() || v;
                        renderGui = renderGuiCheckbox.setEnabled(!v).isChecked() || v;
                        useReadPixels = useReadPixelsCheckbox.setEnabled(!v).isChecked() || v;
                        reloadFrameAndPrograms();
                    }),
                    singlePassCheckbox = new ConfigCheckbox("Single Pass", singlePass, v -> {
                        singlePass = v;
                        if (v) {
                            zeroPassCheckbox.setChecked(true).onClick();
                        }
                        reloadFrameAndPrograms();
                    }),
                    new ConfigCheckbox("Tessellation Shader", tessellationShader, v -> {
                        tessellationShader = v;
                        reloadFrameAndPrograms();
                    }),
                    new GuiPanel().addElements(new HorizontalLayout.Data(0.5), maxTessLevelField, new GuiLabel().setText(" Max Tessellation Level")),
                    new ConfigCheckbox("Geometry Shader Instancing", geometryShaderInstancing, v -> {
                        geometryShaderInstancing = v;
                        reloadFrameAndPrograms();
                    }),
                    new ConfigCheckbox("Always use GS Instancing", alwaysUseGeometryShaderInstancing, v -> {
                        alwaysUseGeometryShaderInstancing = v;
                        reloadFrameAndPrograms();
                    }),
                    new ConfigCheckbox("draw*Instanced", drawInstanced, v -> {
                        drawInstanced = v;
                        reloadFrameAndPrograms();
                    }),
                    new ConfigCheckbox("Always tessellate chunks", alwaysTessellateChunks, v -> alwaysTessellateChunks = v),
                    new ConfigCheckbox("Never tessellate chunks", neverTessellateChunks, v -> neverTessellateChunks = v),
                    new ConfigCheckbox("Tessellate gui", tessellateGui, v -> tessellateGui = v),
                    new ConfigCheckbox("Tessellate fonts", tessellateFonts, v -> tessellateFonts = v),
                    new ConfigCheckbox("Skip fonts", skipFonts, v -> skipFonts = v),
                    renderWorldCheckbox = new ConfigCheckbox("Render world", renderWorld, v -> renderWorld = v),
                    renderGuiCheckbox = new ConfigCheckbox("Render gui", renderGui, v -> renderGui = v),
                    new ConfigCheckbox("Compose frames into one", compose, v -> compose = v),
                    new ConfigCheckbox("Download final frame", transfer, v -> transfer = v),
                    useReadPixelsCheckbox = new ConfigCheckbox("Use glReadPixels", useReadPixels, v -> useReadPixels = v),
                    new GuiPanel().addElements(new HorizontalLayout.Data(0.5), pbosField, new GuiLabel().setText(" PBOs"))
            );

    {
        // Propagate en-/disabled state
        zeroPassCheckbox.setChecked(zeroPass);

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

    private void reloadFrameAndPrograms() {
        VR180FrameCapturer capturer = VR180FrameCapturer.getCurrent();
        if (capturer != null) {
            capturer.recreateFrame();
            capturer.reloadPrograms();
        }
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

        glDrawArrays.setText(String.format("%,d", glDrawArraysCounter));
        programSwitches.setText(String.format("%,d", programSwitchesCounter));
        glDrawArraysCounter = 0;
        programSwitchesCounter = 0;

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
