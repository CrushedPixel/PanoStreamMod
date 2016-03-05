package com.replaymod.panostream.gui;

import com.replaymod.panostream.settings.PanoStreamSettings;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.layout.GridLayout;
import lombok.Getter;

public class GuiPanoStreamSettings extends GuiScreen {

    public GuiPanoStreamSettings(final net.minecraft.client.gui.GuiScreen parent, PanoStreamSettings panoStreamSettings) {

        GuiPanel allElements = new GuiPanel(this).setLayout(new GridLayout().setSpacingX(10).setSpacingY(10).setColumns(3));

        /*
        new SettingsRow("") {
            @Override
            void applySetting() {
                panoStreamSettings.rtmpServer.getStringValue();
            }
        }.addTo(allElements);
        */
    }

    private abstract class SettingsRow {

        @Getter
        private final GuiLabel nameLabel;

        @Getter
        private final GuiElement inputElement;

        @Getter
        private final GuiButton infoButton;

        public SettingsRow(String optionNameI18N, String optionDescriptionI18N, GuiElement inputElement) {
            this.inputElement = inputElement;

            this.nameLabel = new GuiLabel().setI18nText(optionNameI18N);

            this.infoButton = new GuiButton()
                    .setTooltip(new GuiTooltip().setI18nText(optionDescriptionI18N))
                    .setLabel("i"); //TODO: Replace with fancy texture "i"
        }

        abstract void applySetting();

        public void addTo(GuiPanel guiPanel) {
            guiPanel.addElements(null, nameLabel, inputElement, infoButton);
        }

    }

}
