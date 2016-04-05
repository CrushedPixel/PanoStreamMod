package com.replaymod.panostream.input;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.gui.GuiPanoStreamSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomKeyBindings {

    private static Minecraft mc = Minecraft.getMinecraft();

    private final List<KeyBinding> customKeyBindings = new ArrayList<>();

    public final KeyBinding keyBindPanoStreamSettings = new KeyBinding("panostream.input.keybindpanostreamsettings",
            Keyboard.KEY_O, "panostream.title");

    public CustomKeyBindings() {
        customKeyBindings.add(keyBindPanoStreamSettings);
    }

    public CustomKeyBindings register() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));
        bindings.addAll(customKeyBindings);

        mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);

        mc.gameSettings.loadOptions();

        FMLCommonHandler.instance().bus().register(this);

        return this;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if(mc.currentScreen == null && keyBindPanoStreamSettings.isPressed()) {
            new GuiPanoStreamSettings(null, PanoStreamMod.instance.getPanoStreamSettings()).display();
        }
    }

}
