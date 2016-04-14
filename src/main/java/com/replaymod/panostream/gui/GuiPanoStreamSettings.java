package com.replaymod.panostream.gui;

import com.replaymod.panostream.settings.PanoStreamSettings;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class GuiPanoStreamSettings extends AbstractGuiScreen<GuiPanoStreamSettings> {

    // a constant to align GuiLabel elements to be centered
    // next to GuiButtons, GuiTextFields etc (which have a height of 20px)
    private final double TEXT_ALIGNMENT = 1 - Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 20d;

    public GuiPanoStreamSettings(final net.minecraft.client.gui.GuiScreen parent,
                                 PanoStreamSettings panoStreamSettings) {

        final List<SettingsRow> settingRows = new ArrayList<>();

        final GuiPanel mainPanel = new GuiPanel(this).setLayout(
                new GridLayout().setCellsEqualSize(false).setSpacingX(10).setSpacingY(10).setColumns(3));

        new SettingsRow("panostream.gui.settings.rtmpaddress",
                new GuiTextField().setText(panoStreamSettings.rtmpServer.getStringValue())) {

            @Override
            void applySetting() {
                panoStreamSettings.rtmpServer.setStringValue(((GuiTextField) inputElement).getText());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.resolution",
                new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))) {

            private GuiNumberField widthField, heightField;

            {
                GuiPanel panel = (GuiPanel)inputElement;

                widthField = new GuiNumberField().setValue(panoStreamSettings.videoWidth.getIntValue()).setWidth(50)
                        .setMinValue(100)
                        .setMaxValue(10000)
                        .onTextChanged(obj -> {
                            heightField.setValue(widthField.getInteger() / 2);
                            widthField.setValue(heightField.getInteger() * 2);
                        });

                heightField = new GuiNumberField().setValue(panoStreamSettings.videoHeight.getIntValue()).setWidth(50)
                        .setMinValue(50)
                        .setMaxValue(5000)
                        .onTextChanged(obj -> {
                            widthField.setValue(heightField.getInteger() * 2);
                        });

                panel.addElements(null, widthField)
                    .addElements(new HorizontalLayout.Data(TEXT_ALIGNMENT), new GuiLabel().setText("*"))
                    .addElements(null, heightField);
            }

        @Override
            void applySetting() {
                panoStreamSettings.videoWidth.setIntValue(widthField.getInteger());
                panoStreamSettings.videoHeight.setIntValue(heightField.getInteger());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.fps",
                new GuiNumberField().setValue(panoStreamSettings.fps.getIntValue()).setWidth(50)) {

            @Override
            void applySetting() {
                panoStreamSettings.fps.setIntValue(((GuiNumberField) inputElement).getInteger());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        new SettingsRow("panostream.gui.settings.ffmpeg",
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5))
                        .addElements(null,
                                new GuiTextField().setMaxLength(1000)
                                        .setText(panoStreamSettings.ffmpegCommand.getStringValue()).setWidth(50),
                                new GuiTextField().setMaxLength(1000)
                                        .setText(panoStreamSettings.ffmpegArgs.getStringValue()).setWidth(100))) {

            @Override
            void applySetting() {
                GuiPanel panel = (GuiPanel)inputElement;
                Iterator<GuiElement> it = panel.getElements().keySet().iterator();

                GuiTextField ffmpegCommand = (GuiTextField)it.next();
                GuiTextField ffmpegArgs = (GuiTextField)it.next();

                panoStreamSettings.ffmpegCommand.setStringValue(ffmpegCommand.getText());
                panoStreamSettings.ffmpegArgs.setStringValue(ffmpegArgs.getText());
            }

        }.addToCollection(settingRows).addTo(mainPanel);

        final GuiButton doneButton = new GuiButton().setI18nLabel("gui.done").onClick(() -> {
            for(SettingsRow settingsRow : settingRows) {
                settingsRow.applySetting();
            }
            panoStreamSettings.save();
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }).setWidth(200);

        addElements(null, mainPanel, doneButton);

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
        private final GuiButton infoButton;

        public SettingsRow(String optionI18NKey, GuiElement inputElement) {
            this.inputElement = inputElement;

            this.nameLabel = new GuiLabel().setI18nText(optionI18NKey+".title");

            this.infoButton = new GuiButton()
                    .setTooltip(new GuiTooltip().setI18nText(optionI18NKey+".info"))
                    .setWidth(20).setLabel("i"); //TODO: Replace with fancy texture "i"
        }

        abstract void applySetting();

        public void addTo(GuiPanel guiPanel) {
            guiPanel.addElements(new GridLayout.Data(0, TEXT_ALIGNMENT), nameLabel);
            guiPanel.addElements(null, inputElement, infoButton);
        }

        public SettingsRow addToCollection(Collection<SettingsRow> collection) {
            collection.add(this);
            return this;
        }

    }

}
