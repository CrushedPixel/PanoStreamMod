package com.replaymod.panostream.mixin;

import com.replaymod.panostream.capture.vr180.VR180ScreenshotCapturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow @Final private static Logger LOGGER;
    private static VR180ScreenshotCapturer screenshotCapturer;

    @Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;"))
    private ITextComponent redirectScreenshot(File gameDirectory, int width, int height, Framebuffer buffer) {
        if(screenshotCapturer == null) {
            screenshotCapturer = new VR180ScreenshotCapturer();
            screenshotCapturer.register();
        }

        if(!GuiScreen.isShiftKeyDown() && !GuiScreen.isCtrlKeyDown()) {
            return ScreenShotHelper.saveScreenshot(gameDirectory, width, height, buffer);
        }

        screenshotCapturer.captureScreenshotAsync(gameDirectory);

        return new TextComponentTranslation("panostream.chat.screenshot.saving");
    }

    @Redirect(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create(Lorg/lwjgl/opengl/PixelFormat;)V", remap = false))
    private void createDisplay(PixelFormat pixelFormat) throws LWJGLException {
        try {
            Display.create(pixelFormat, new ContextAttribs(4, 0).withProfileCompatibility(true));
        } catch (LWJGLException e) {
            LOGGER.error("Failed to create OpenGL 4.0 Compatibility Profile, Tessellation Shaders will be unavailable:", e);
            try {
                Display.create(pixelFormat, new ContextAttribs(3, 2).withProfileCompatibility(true));
            } catch (LWJGLException e1) {
                LOGGER.error("Failed to create OpenGL 3.2 Compatibility Profile, Geometry Shaders will be unavailable:", e1);
                Display.create(pixelFormat);
            }
        }
    }
}
