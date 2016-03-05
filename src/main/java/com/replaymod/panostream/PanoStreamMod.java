package com.replaymod.panostream;

import com.replaymod.panostream.capture.PanoramicFrameCapturer;
import com.replaymod.panostream.settings.PanoStreamSettings;
import com.replaymod.panostream.stream.VideoStreamer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.util.Dimension;

@Mod(modid = PanoStreamMod.MODID, useMetadata = true)
public class PanoStreamMod {

    public static final String MODID = "panostream";

    @Mod.Instance(value = MODID)
    public static PanoStreamMod instance;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private PanoramicFrameCapturer panoramicFrameCapturer;

    @Getter
    private VideoStreamer videoStreamer;

    @Getter
    private PanoStreamSettings panoStreamSettings;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        panoStreamSettings = new PanoStreamSettings(new Configuration(event.getSuggestedConfigurationFile()));
        panoStreamSettings.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        videoStreamer = new VideoStreamer(new Dimension(panoStreamSettings.videoWidth.getIntValue(),
                panoStreamSettings.videoHeight.getIntValue()), panoStreamSettings.fps.getIntValue(),
                panoStreamSettings.rtmpServer.getStringValue());

        panoramicFrameCapturer = new PanoramicFrameCapturer(panoStreamSettings.videoHeight.getIntValue() / 2,
                videoStreamer).register();
    }

}
