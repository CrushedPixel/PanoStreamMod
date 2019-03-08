package com.replaymod.panostream.capture;


import com.replaymod.panostream.capture.equi.CaptureState;
import com.replaymod.panostream.gui.GuiDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.client.renderer.OpenGlHelper.*;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_VALIDATE_STATUS;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glValidateProgram;

public class Program {
    private static ThreadLocal<Program> boundProgram = new ThreadLocal<>();

    public static Program getBoundProgram() {
        return boundProgram.get();
    }

    private final boolean hasGeometryShader;
    private final boolean hasTessEvalShader;
    private final int program;
    private final CachedUniforms cachedUniforms;

    public Program(ResourceLocation vertexShader, ResourceLocation fragmentShader, String...defines) throws Exception {
        this(vertexShader, null, fragmentShader, defines);
    }

    public Program(ResourceLocation vertexShader, ResourceLocation geometryShader, ResourceLocation fragmentShader, String...defines) throws Exception {
        this(vertexShader, null, null, geometryShader, fragmentShader, defines);
    }

    public Program(ResourceLocation vertexShader,
                   ResourceLocation tessellationControlShader,
                   ResourceLocation tessellationEvaluationShader,
                   ResourceLocation geometryShader,
                   ResourceLocation fragmentShader,
                   String...defines) throws Exception {
        int vertShader = createShader(vertexShader, defines, GL_VERTEX_SHADER);
        int tessControlShader = 0;
        if (tessellationControlShader != null) {
            tessControlShader = createShader(tessellationControlShader, defines, GL40.GL_TESS_CONTROL_SHADER);
        }
        int tessEvalShader = 0;
        if (tessellationEvaluationShader != null) {
            hasTessEvalShader = true;
            tessEvalShader = createShader(tessellationEvaluationShader, defines, GL40.GL_TESS_EVALUATION_SHADER);
        } else {
            hasTessEvalShader = false;
        }
        int geomShader = 0;
        if (geometryShader != null) {
            hasGeometryShader = true;
            geomShader = createShader(geometryShader, defines, GL32.GL_GEOMETRY_SHADER);
        } else {
            hasGeometryShader = false;
        }
        int fragShader = createShader(fragmentShader, defines, GL_FRAGMENT_SHADER);

        program = glCreateProgram();
        if (program == 0) {
            throw new Exception("glCreateProgramObjectARB failed");
        }

        glAttachShader(program, vertShader);
        if (tessellationControlShader != null) {
            glAttachShader(program, tessControlShader);
        }
        if (tessellationEvaluationShader != null) {
            glAttachShader(program, tessEvalShader);
        }
        if (geometryShader != null) {
            glAttachShader(program, geomShader);
        }
        glAttachShader(program, fragShader);

        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new Exception("Error linking: " + getProgramLogInfo(program));
        }

        glValidateProgram(program);
        if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            throw new Exception("Error validating: " + getProgramLogInfo(program));
        }

        use();
        cachedUniforms = new CachedUniforms();
        stopUsing();
    }

    private int createShader(ResourceLocation resourceLocation, String[] defines, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = glCreateShader(shaderType);

            if (shader == 0) {
                Util.checkGLError();
                throw new Exception("glCreateShader failed but no error was set");
            }

            List<String> lines = loadShaderSource(resourceLocation);
            List<String> result = new ArrayList<>();
            String versionDirective = lines.remove(0);
            if (shaderType == GL32.GL_GEOMETRY_SHADER && GuiDebug.instance.geometryShaderInstancing) {
                versionDirective = "#version 400";
            }
            result.add(versionDirective);
            result.addAll(Arrays.asList(defines));
            result.addAll(lines);
            glShaderSource(shader, String.join("\n", result));
            glCompileShader(shader);

            if (glGetShaderi(shader, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: "
                        + glGetShaderInfoLog(shader, glGetShaderi(shader, GL_INFO_LOG_LENGTH)));

            return shader;
        } catch (Exception exc) {
            glDeleteShader(shader);
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

    private static String getProgramLogInfo(int program) {
        return glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH));
    }

    public void use() {
        glUseProgram(program);
        CaptureState.setGeometryShader(hasGeometryShader);
        CaptureState.setTessEvalShader(hasTessEvalShader);
        boundProgram.set(this);
    }

    public void stopUsing() {
        if (boundProgram.get() != this) {
            throw new IllegalStateException("program is not currently bound");
        }
        glUseProgram(0);
        CaptureState.setGeometryShader(false);
        CaptureState.setTessEvalShader(false);
        boundProgram.set(null);
    }

    public void delete() {
        glDeleteProgram(program);
    }

    public CachedUniforms uniforms() {
        return cachedUniforms;
    }

    public Uniform getUniformVariable(String name) {
        return new Uniform(glGetUniformLocation(program, name));
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
            glUniform1i(location, bool ? GL11.GL_TRUE : GL11.GL_FALSE);
        }

        public void set(int integer) {
            glUniform1i(location, integer);
        }

        public void set(float flt) {
            glUniform1f(location, flt);
        }
    }

    /**
     * Pre-cached uniform locations for commonly used uniform names.
     */
    public class CachedUniforms {
        public final Uniform renderPass = getUniformVariable("renderPass");
    }
}