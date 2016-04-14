package com.replaymod.panostream.utils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class Registerable<T> {

    public T register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        return getThis();
    }

    public abstract T getThis();

}
