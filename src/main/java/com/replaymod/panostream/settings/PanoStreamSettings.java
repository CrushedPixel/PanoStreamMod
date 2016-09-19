package com.replaymod.panostream.settings;

import eu.crushedpixel.minecraft.simpleconfig.BooleanSetting;
import eu.crushedpixel.minecraft.simpleconfig.ConfigSettings;
import eu.crushedpixel.minecraft.simpleconfig.IntegerSetting;
import eu.crushedpixel.minecraft.simpleconfig.StringSetting;
import net.minecraftforge.common.config.Configuration;

public class PanoStreamSettings extends ConfigSettings {
    
    public IntegerSetting fps = new IntegerSetting(30);

    public IntegerSetting videoWidth = new IntegerSetting(2160);

    public IntegerSetting videoHeight = new IntegerSetting(1080);

    public StringSetting ffmpegCommand = new StringSetting("ffmpeg");

    public StringSetting ffmpegArgs = new StringSetting("-re -f rawvideo -pix_fmt rgb24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - " +
            "-f flv -qmin 2 -qmax 25 -rtmp_buffer 100 -rtmp_live live %DESTINATION%");

    public StringSetting rtmpServer = new StringSetting("rtmp://localhost/panostream/live");

    public BooleanSetting stabilizeOutput = new BooleanSetting(true);

    public PanoStreamSettings(Configuration config) {
        super(config);
    }

}
