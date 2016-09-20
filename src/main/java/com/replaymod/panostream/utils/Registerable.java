package com.replaymod.panostream.utils;

import net.minecraftforge.common.MinecraftForge;

public abstract class Registerable<T> {

    public T register() {
        MinecraftForge.EVENT_BUS.register(this);
        return getThis();
    }

    public abstract T getThis();

}
