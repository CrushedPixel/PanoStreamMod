package com.replaymod.panostream.settings;

import eu.crushedpixel.minecraft.simpleconfig.BooleanSetting;
import eu.crushedpixel.minecraft.simpleconfig.ConfigSettings;
import eu.crushedpixel.minecraft.simpleconfig.DoubleSetting;
import eu.crushedpixel.minecraft.simpleconfig.IntegerSetting;
import eu.crushedpixel.minecraft.simpleconfig.StringSetting;
import net.minecraftforge.common.config.Configuration;

public class PanoStreamSettings extends ConfigSettings {

    public IntegerSetting fps = new IntegerSetting(30);

    public IntegerSetting videoWidth = new IntegerSetting(1080);

    public IntegerSetting videoHeight = new IntegerSetting(2160);

    public StringSetting ffmpegCommand = new StringSetting("ffmpeg");

    public StringSetting ffmpegArgs = new StringSetting("-f rawvideo -pix_fmt bgra -s %WIDTH%x%HEIGHT% -r %FPS% -i - " +
            "-f flv -qmin 2 -qmax 15 -rtmp_buffer 100 -rtmp_live live %DESTINATION%");

    public StringSetting rtmpServer = new StringSetting("rtmp://localhost/panostream/live");

    public BooleanSetting stabilizeOutput = new BooleanSetting(true);

    public BooleanSetting vr180 = new BooleanSetting(true);

    public DoubleSetting ipd = new DoubleSetting(0.25);

    public PanoStreamSettings(Configuration config) {
        super(config);
    }

}
