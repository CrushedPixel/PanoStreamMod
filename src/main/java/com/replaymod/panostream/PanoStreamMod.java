package com.replaymod.panostream;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = PanoStreamMod.MODID, useMetadata = true)
public class PanoStreamMod {

    public static final String MODID = "panostream";

    @Mod.Instance(value = MODID)
    public static PanoStreamMod instance;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private PanoramicFrameCapturer panoramicFrameCapturer;

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        panoramicFrameCapturer = new PanoramicFrameCapturer(1024).register();
        panoramicFrameCapturer.setActive(true);
    }

}
