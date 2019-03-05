package com.replaymod.panostream.gui;

import com.replaymod.panostream.settings.PanoStreamSettings;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiNumberField;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.GuiTexturedButton;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.function.Focusable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.Utils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GuiPanoStreamSettings extends AbstractGuiScreen<GuiPanoStreamSettings> {

    // a constant to align GuiLabel elements to be centered
    // next to GuiButtons, GuiTextFields etc (which have a height of 20px)
    private final double TEXT_ALIGNMENT = 1 - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 20d;

    private GuiNumberField widthField, heightField;

    public GuiPanoStreamSettings(final net.minecraft.client.gui.GuiScreen parent,
                                 PanoStreamSettings panoStreamSettings) {

        final List<SettingsRow> settingRows = new ArrayList<>();

        final GuiPanel mainPanel = new GuiPanel(this).setLayout(
                new GridLayout().setCellsEqualSize(false).setSpacingX(10).setSpacingY(10).setColumns(3));

        GuiDropdownMenu<String> modeDropdown = new GuiDropdownMenu<String>()
                .setSelected(panoStreamSettings.vr180.getValue() ? 1 : 0)
                .setValues(I18n.format("panostream.gui.settings.mode.360"), I18n.format("panostream.gui.settings.mode.vr180"))
                .onSelection(i -> {
                    if (i == 0 ^ heightField.getInteger() == widthField.getInteger() / 2) {
                        int tmp = widthField.getInteger();
                        widthField.setValue(heightField.getInteger());
                        heightField.setValue(tmp);
                    }
                });
        new SettingsRow("panostream.gui.settings.mode", modeDropdown) {

            @Override
            void applySetting() {
                panoStreamSettings.vr180.setValue(((GuiDropdownMenu) inputElement).getSelected() == 1);
            }

            @Override
            void restoreDefault() {
                ((GuiDropdownMenu) inputElement).setSelected(panoStreamSettings.vr180.getDefault() ? 1 : 0);
            }

        }.addToCollection(settingRows).addTo(mainPanel, new GridLayout.Data(0, TEXT_ALIGNMENT));

        new SettingsRow("panostream.gui.settings.stabilize",
                new GuiCheckbox().setChecked(panoStreamSettings.stabilizeOutput.getValue())) {

            @Override
            void applySetting() {
                panoStreamSettings.stabilizeOutput.setValue(((GuiCheckbox) inputElement).isChecked());
            }

            @Override
            void restoreDefault() {
                ((GuiCheckbox) inputElement).setChecked(panoStreamSettings.stabilizeOutput.getDefault());
            }

        }.addToCollection(settingRows).addTo(mainPanel, new GridLayout.Data(0, TEXT_ALIGNMENT));

        new SettingsRow("panostream.gui.settings.rtmpaddress",
                new GuiTextField().setHeight(20).setMaxLength(1000).setText(panoStreamSettings.rtmpServer.getValue())) {

            @Override
            void applySetting() {
                panoStreamSettings.rtmpServer.setValue(((GuiTextField) inputElement).getText());
            }

            @Override
            void restoreDefault() {
                ((GuiTextField) inputElement).setText(panoStreamSettings.rtmpServer.getDefault());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.resolution",
                new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))) {


            {
                GuiPanel panel = (GuiPanel)inputElement;

                widthField = new GuiNumberField().setValue(panoStreamSettings.videoWidth.getValue()).setWidth(50)
                        .setMinValue(0)
                        .setMaxValue(100000)
                        .setHeight(20)
                        .setValidateOnFocusChange(true)
                        .onTextChanged(obj -> {
                            if (modeDropdown.getSelected() == 0) {
                                heightField.setValue(widthField.getInteger() / 2);
                                widthField.setValue(heightField.getInteger() * 2);
                            } else {
                                heightField.setValue(widthField.getInteger() * 2);
                            }
                        });

                heightField = new GuiNumberField().setValue(panoStreamSettings.videoHeight.getValue()).setWidth(50)
                        .setMinValue(0)
                        .setMaxValue(50000)
                        .setHeight(20)
                        .setValidateOnFocusChange(true)
                        .onFocusChange(focused -> {
                            if (heightField.getInteger() > 5000) heightField.setValue(5000);
                            if (heightField.getInteger() < 100) heightField.setValue(100);
                        })
                        .onTextChanged(obj -> {
                            if (modeDropdown.getSelected() == 0) {
                                widthField.setValue(heightField.getInteger() * 2);
                            } else {
                                widthField.setValue(heightField.getInteger() / 2);
                                heightField.setValue(widthField.getInteger() * 2);
                            }
                        });

                panel.addElements(null, widthField)
                        .addElements(new HorizontalLayout.Data(TEXT_ALIGNMENT), new GuiLabel().setText("*"))
                        .addElements(null, heightField);
            }

            @Override
            void applySetting() {
                panoStreamSettings.videoWidth.setValue(widthField.getInteger());
                panoStreamSettings.videoHeight.setValue(heightField.getInteger());
            }

            @Override
            void restoreDefault() {
                widthField.setValue(panoStreamSettings.videoWidth.getDefault());
                heightField.setValue(panoStreamSettings.videoHeight.getDefault());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.fps",
                new GuiNumberField().setValue(panoStreamSettings.fps.getValue())
                        .setWidth(50)
                        .setHeight(20)
                        .setMinValue(0)
                        .setValidateOnFocusChange(true)) {

            @Override
            void applySetting() {
                panoStreamSettings.fps.setValue(((GuiNumberField) inputElement).getInteger());
            }

            @Override
            void restoreDefault() {
                ((GuiNumberField)inputElement).setValue(panoStreamSettings.fps.getDefault());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.ffmpeg",
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5))
                        .addElements(null,
                                new GuiTextField().setMaxLength(1000)
                                        .setText(panoStreamSettings.ffmpegCommand.getValue()).setWidth(50).setHeight(20),
                                new GuiTextField().setMaxLength(1000)
                                        .setText(panoStreamSettings.ffmpegArgs.getValue()).setWidth(100).setHeight(20))) {

            private GuiTextField ffmpegCommand, ffmpegArgs;

            {
                GuiPanel panel = (GuiPanel)inputElement;
                Iterator<GuiElement> it = panel.getElements().keySet().iterator();

                ffmpegCommand = (GuiTextField)it.next();
                ffmpegArgs = (GuiTextField)it.next();
            }

            @Override
            void applySetting() {
                panoStreamSettings.ffmpegCommand.setValue(ffmpegCommand.getText());
                panoStreamSettings.ffmpegArgs.setValue(ffmpegArgs.getText());
            }

            @Override
            void restoreDefault() {
                ffmpegCommand.setText(panoStreamSettings.ffmpegCommand.getDefault());
                ffmpegArgs.setText(panoStreamSettings.ffmpegArgs.getDefault());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        final GuiButton doneButton = new GuiButton().setI18nLabel("gui.done").onClick(() -> {
            for(SettingsRow settingsRow : settingRows) {
                settingsRow.applySetting();
            }
            panoStreamSettings.save();
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }).setWidth(200);

        List<Focusable> toLink = new LinkedList<Focusable>();
        addFocusablesToList(mainPanel, toLink);

        Utils.link(toLink.toArray(new Focusable[toLink.size()]));

        addElements(null, mainPanel, doneButton);

        if (GuiDebug.instance.isVisible()) {
            mainPanel.addElements(null, new GuiButton().setLabel("Debug").onClick(() -> {
                GuiDebug.instance.setMouseVisible(true);
            }));
        }

        setLayout(new CustomLayout<GuiPanoStreamSettings>() {
            @Override
            protected void layout(GuiPanoStreamSettings container, int width, int height) {
                int mainPanelY = 30;
                int spacing = 5;
                pos(mainPanel, (width - mainPanel.getMinSize().getWidth()) / 2,
                        mainPanelY);
                pos(doneButton, (width - doneButton.getMinSize().getWidth()) / 2,
                        mainPanelY + mainPanel.getMinSize().getHeight() + spacing);
            }
        });

        setTitle(new GuiLabel().setI18nText("panostream.gui.settings.title"));
    }

    private void addFocusablesToList(GuiElement element, List<Focusable> list) {
        if(element instanceof Focusable) {
            list.add((Focusable)element);
        } else if(element instanceof GuiContainer) {
            GuiContainer container = (GuiContainer)element;
            for(GuiElement guiElement : (Iterable<GuiElement>) container.getChildren()) {
                addFocusablesToList(guiElement, list);
            }
        }
    }

    @Override
    protected GuiPanoStreamSettings getThis() {
        return this;
    }

    private abstract class SettingsRow {

        @Getter
        private final GuiLabel nameLabel;

        @Getter
        protected final GuiElement inputElement;

        @Getter
        private final GuiTexturedButton infoButton;

        public SettingsRow(String optionI18NKey, GuiElement inputElement) {
            this.inputElement = inputElement;

            this.nameLabel = new GuiLabel().setI18nText(optionI18NKey+".title");

            this.infoButton = new GuiTexturedButton()
                    .setTooltip(new GuiTooltip().setI18nText(optionI18NKey+".info"))
                    .setWidth(20).setHeight(20).setTexture(GuiOverlays.OVERLAY_RESOURCE, GuiOverlays.TEXTURE_SIZE)
                    .setTexturePos(16, 0, 16, 20)
                    .onClick(SettingsRow.this::restoreDefault);
        }

        abstract void applySetting();
        abstract void restoreDefault();

        public void addTo(GuiPanel guiPanel) {
            addTo(guiPanel, null);
        }

        public void addTo(GuiPanel guiPanel, GridLayout.Data layoutData) {
            guiPanel.addElements(new GridLayout.Data(0, TEXT_ALIGNMENT), nameLabel);
            guiPanel.addElements(layoutData, inputElement, infoButton);
        }

        public SettingsRow addToCollection(Collection<SettingsRow> collection) {
            collection.add(this);
            return this;
        }

    }

}
