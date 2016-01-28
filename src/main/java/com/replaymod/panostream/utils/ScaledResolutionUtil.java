package com.replaymod.panostream.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class ScaledResolutionUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void setWorldAndResolution(GuiScreen guiScreen) {
        setWorldAndResolution(guiScreen, mc.displayWidth, mc.displayHeight);
    }

    public static void setWorldAndResolution(GuiScreen guiScreen, int displayWidth, int displayHeight) {
        ScaledResolution scaledResolution = new ScaledResolution(mc, displayWidth, displayHeight);
        if(guiScreen != null) guiScreen.setWorldAndResolution(mc, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }
}
