package com.replaymod.panostream;

import com.replaymod.panostream.capture.PanoramicFrameCapturer;
import com.replaymod.panostream.stream.VideoStreamer;
import com.replaymod.panostream.utils.FrameSizeUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = PanoStreamMod.MODID, useMetadata = true)
public class PanoStreamMod {

    public static final String MODID = "panostream";

    public static final int DEFAULT_FPS = 30;
    //public static final int DEFAULT_FRAMESIZE = 1080; //for 4k
    public static final int DEFAULT_FRAMESIZE = 540; //for 1080p
    public static final String DEFAULT_DESTINATION = "rtmp://127.0.0.1/panostream/minecraft";
    //public static final String DEFAULT_DESTINATION = "equi.mp4";

    @Mod.Instance(value = MODID)
    public static PanoStreamMod instance;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private PanoramicFrameCapturer panoramicFrameCapturer;

    @Getter
    private VideoStreamer videoStreamer;

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        videoStreamer = new VideoStreamer(FrameSizeUtil.composedFrameSize(DEFAULT_FRAMESIZE), DEFAULT_FPS, DEFAULT_DESTINATION);
        panoramicFrameCapturer = new PanoramicFrameCapturer(DEFAULT_FRAMESIZE, videoStreamer).register();
    }

}
