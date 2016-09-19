package com.replaymod.panostream;

import com.replaymod.panostream.gui.GuiOverlays;
import com.replaymod.panostream.input.CustomKeyBindings;
import com.replaymod.panostream.settings.PanoStreamSettings;
import com.replaymod.panostream.stream.VideoStreamer;
import lombok.Getter;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = PanoStreamMod.MODID, useMetadata = true)
public class PanoStreamMod {

    public static final String MODID = "panostream";

    @Mod.Instance(value = MODID)
    public static PanoStreamMod instance;

    @Getter
    private VideoStreamer videoStreamer;

    @Getter
    private PanoStreamSettings panoStreamSettings;

    @Getter
    private CustomKeyBindings customKeyBindings;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        panoStreamSettings = new PanoStreamSettings(new Configuration(event.getSuggestedConfigurationFile()));
        panoStreamSettings.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        videoStreamer = new VideoStreamer();

        customKeyBindings = new CustomKeyBindings().register();

        new GuiOverlays().register();
    }

}
