package com.replaymod.panostream.input;

import com.replaymod.panostream.PanoStreamMod;
import com.replaymod.panostream.gui.GuiPanoStreamSettings;
import com.replaymod.panostream.utils.Registerable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomKeyBindings extends Registerable<CustomKeyBindings> {

    private static Minecraft mc = Minecraft.getMinecraft();

    private final List<CustomKeyBinding> customKeyBindings = new ArrayList<>();

    public final CustomKeyBinding keyBindPanoStreamSettings = new CustomKeyBinding("panostream.input.keybindpanostreamsettings",
            Keyboard.KEY_O, "panostream.title") {
        @Override
        public void onPressed() {
            new GuiPanoStreamSettings(null, PanoStreamMod.instance.getPanoStreamSettings()).display();
        }
    };

    public final CustomKeyBinding keyBindToggleStreaming = new CustomKeyBinding("panostream.input.keybindtogglestreaming",
            Keyboard.KEY_F4, "panostream.title") {
        @Override
        public void onPressed() {
            PanoStreamMod.instance.getVideoStreamer().toggleStream();
        }
    };

    public CustomKeyBindings() {
        customKeyBindings.add(keyBindPanoStreamSettings);
        customKeyBindings.add(keyBindToggleStreaming);
    }

    @Override
    public CustomKeyBindings register() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));
        bindings.addAll(customKeyBindings);

        mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);

        mc.gameSettings.loadOptions();

        return super.register();
    }

    @SubscribeEvent
    public void onKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        onKeyInput(true);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        onKeyInput(false);
    }

    public void onKeyInput(boolean guiScreen) {
        for(CustomKeyBinding binding : customKeyBindings) {
            binding.press(guiScreen);
        }
    }

    @Override
    public CustomKeyBindings getThis() {
        return this;
    }

}
