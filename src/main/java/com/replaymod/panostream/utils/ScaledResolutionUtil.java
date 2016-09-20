package com.replaymod.panostream.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class ScaledResolutionUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static ScaledResolution createScaledResolution(int width, int height) {
        //temporarily replace Minecraft's width and height with our custom width and height,
        //as Minecraft 1.9 removed the ScaledResolution constructor accepting any width and height value
        int widthBefore = mc.displayWidth;
        int heightBefore = mc.displayHeight;
        mc.displayWidth = width;
        mc.displayHeight = height;

        ScaledResolution scaledResolution = new ScaledResolution(mc);

        mc.displayWidth = widthBefore;
        mc.displayHeight = heightBefore;

        return scaledResolution;
    }

    public static void setWorldAndResolution(GuiScreen guiScreen, int displayWidth, int displayHeight) {
        ScaledResolution scaledResolution = createScaledResolution(displayWidth, displayHeight);
        if(guiScreen != null) guiScreen.setWorldAndResolution(mc, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }
}
