package com.replaymod.panostream;

import lombok.Getter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = PanoStreamMod.MODID, useMetadata = true)
public class PanoStreamMod {

    public static final String MODID = "panostream";

    @Mod.Instance(value = MODID)
    public static PanoStreamMod instance;

    @Getter
    private PanoramicFrameCapturer panoramicFrameCapturer = new PanoramicFrameCapturer(1024);

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(panoramicFrameCapturer);
        panoramicFrameCapturer.setActive(true);
    }

}
