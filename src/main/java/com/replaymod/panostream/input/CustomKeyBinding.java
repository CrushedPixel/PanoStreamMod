package com.replaymod.panostream.input;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public abstract class CustomKeyBinding extends KeyBinding {

    public CustomKeyBinding(String description, int keyCode, String category) {
        super(description, keyCode, category);
    }

    public abstract void onPressed();

    public boolean checkPressed(boolean guiScreen) {
        return isPressed() || (guiScreen && Keyboard.isKeyDown(getKeyCode()));
    }

    public void press(boolean guiScreen) {
        if(checkPressed(guiScreen)) onPressed();
    }

}
