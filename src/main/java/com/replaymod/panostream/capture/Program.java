package com.replaymod.panostream.capture;


import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.ARBShaderObjects.*;

public class Program {
    private static ThreadLocal<Program> boundProgram = new ThreadLocal<>();

    public static Program getBoundProgram() {
        return boundProgram.get();
    }

    private final boolean hasGeometryShader;
    private final int program;
    private final CachedUniforms cachedUniforms;

    public Program(ResourceLocation vertexShader, ResourceLocation fragmentShader, String...defines) throws Exception {
        this(vertexShader, null, fragmentShader, defines);
    }

    public Program(ResourceLocation vertexShader, ResourceLocation geometryShader, ResourceLocation fragmentShader, String...defines) throws Exception {
        int vertShader = createShader(vertexShader, defines, ARBVertexShader.GL_VERTEX_SHADER_ARB);
        int geomShader = 0;
        if (geometryShader != null) {
            hasGeometryShader = true;
            geomShader = createShader(geometryShader, defines, GL32.GL_GEOMETRY_SHADER);
        } else {
            hasGeometryShader = false;
        }
        int fragShader = createShader(fragmentShader, defines, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        program = glCreateProgramObjectARB();
        if (program == 0) {
            throw new Exception("glCreateProgramObjectARB failed");
        }

        glAttachObjectARB(program, vertShader);
        if (geometryShader != null) {
            glAttachObjectARB(program, geomShader);
        }
        glAttachObjectARB(program, fragShader);

        glLinkProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error linking: " + getLogInfo(program));
        }

        glValidateProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error validating: " + getLogInfo(program));
        }

        cachedUniforms = new CachedUniforms();
    }

    private int createShader(ResourceLocation resourceLocation, String[] defines, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = OpenGlHelper.glCreateShader(shaderType);

            if (shader == 0) {
                Util.checkGLError();
                throw new Exception("glCreateShader failed but no error was set");
            }

            List<String> lines = loadShaderSource(resourceLocation);
            List<String> result = new ArrayList<>();
            String versionDirective = lines.remove(0);
            if (shaderType == GL32.GL_GEOMETRY_SHADER && GuiDebug.instance.singlePass) {
                versionDirective = "#version 400";
            }
            result.add(versionDirective);
            result.addAll(Arrays.asList(defines));
            result.addAll(lines);
            GL20.glShaderSource(shader, String.join("\n", result));
            OpenGlHelper.glCompileShader(shader);

            if (OpenGlHelper.glGetShaderi(shader, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        } catch (Exception exc) {
            glDeleteObjectARB(shader);
            throw exc;
        }
    }

    private List<String> loadShaderSource(ResourceLocation location) throws IOException {
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
        try (InputStream is = resource.getInputStream()) {
            List<String> lines = new ArrayList<>(
                    Arrays.asList(IOUtils.toString(is, StandardCharsets.UTF_8).split("\n")));
            List<String> result = new ArrayList<>(lines.size());
            int i = 0;
            for (String line : lines) {
                if (line.startsWith("#include ")) {
                    String target = line.substring("#include ".length());
                    ResourceLocation targetLocation = new ResourceLocation(location.getNamespace(), target);
                    result.add("#line 0");
                    result.addAll(loadShaderSource(targetLocation));
                    result.add("#line " + (i + 1));
                } else {
                    result.add(line);
                }
                i++;
            }

            return result;
        }
    }

    private static String getLogInfo(int obj) {
        return glGetInfoLogARB(obj, glGetObjectParameteriARB(obj, GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    public void use() {
        ARBShaderObjects.glUseProgramObjectARB(program);
        CaptureState.setGeometryShader(hasGeometryShader);
        boundProgram.set(this);
    }

    public void stopUsing() {
        if (boundProgram.get() != this) {
            throw new IllegalStateException("program is not currently bound");
        }
        ARBShaderObjects.glUseProgramObjectARB(0);
        CaptureState.setGeometryShader(false);
        boundProgram.set(null);
    }

    public void delete() {
        ARBShaderObjects.glDeleteObjectARB(program);
    }

    public CachedUniforms uniforms() {
        return cachedUniforms;
    }

    public Uniform getUniformVariable(String name) {
        return new Uniform(ARBShaderObjects.glGetUniformLocationARB(program, name));
    }

    public void setUniformValue(String name, int value) {
        getUniformVariable(name).set(value);
    }

    public void setUniformValue(String name, float value) {
        getUniformVariable(name).set(value);
    }

    public class Uniform {
        private final int location;

        public Uniform(int location) {
            this.location = location;
        }

        public void set(boolean bool) {
            ARBShaderObjects.glUniform1iARB(location, bool ? GL11.GL_TRUE : GL11.GL_FALSE);
        }

        public void set(int integer) {
            ARBShaderObjects.glUniform1iARB(location, integer);
        }

        public void set(float flt) {
            ARBShaderObjects.glUniform1fARB(location, flt);
        }
    }

    /**
     * Pre-cached uniform locations for commonly used uniform names.
     */
    public class CachedUniforms {
        public final Uniform leftEye = getUniformVariable("leftEye");
    }
}