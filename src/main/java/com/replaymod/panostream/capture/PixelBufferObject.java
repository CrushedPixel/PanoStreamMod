package com.replaymod.panostream.capture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.Objects;

public class PixelBufferObject {

    @RequiredArgsConstructor
    public enum Usage {
        COPY(ARBPixelBufferObject.GL_STREAM_COPY_ARB, GL15.GL_STREAM_COPY),
        DRAW(ARBPixelBufferObject.GL_STREAM_DRAW_ARB, GL15.GL_STREAM_DRAW),
        READ(ARBPixelBufferObject.GL_STREAM_READ_ARB, GL15.GL_STREAM_READ);

        private final int arb, gl15;
    }

    public static final boolean SUPPORTED = GLContext.getCapabilities().GL_ARB_pixel_buffer_object || GLContext.getCapabilities().OpenGL15;
    private static final boolean arb = !GLContext.getCapabilities().OpenGL15;

    private static ThreadLocal<Integer> bound = new ThreadLocal<Integer>();
    private static ThreadLocal<Integer> mapped = new ThreadLocal<Integer>();

    @Getter
    private final int size;
    private long handle;

    public PixelBufferObject(int size, Usage usage) {
        if (!SUPPORTED) {
            throw new UnsupportedOperationException("PBOs not supported.");
        }

        this.size = size;
        this.handle = arb ? ARBBufferObject.glGenBuffersARB() : GL15.glGenBuffers();

        bind();

        if (arb) {
            ARBBufferObject.glBufferDataARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, size, usage.arb);
        } else {
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, size, usage.gl15);
        }

        unbind();
    }

    private int getHandle() {
        if (handle == -1) {
            throw new IllegalStateException("PBO not allocated.");
        }
        return (int) handle;
    }

    public void bind() {
        if (arb) {
            ARBBufferObject.glBindBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, getHandle());
        } else {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, getHandle());
        }
        bound.set(getHandle());
    }

    public void unbind() {
        checkBound();
        if (arb) {
            ARBBufferObject.glBindBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, 0);
        } else {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        }
        bound.set(0);
    }

    private void checkBound() {
        if (!Objects.equals(getHandle(), bound.get())) {
            throw new IllegalStateException("Buffer not bound.");
        }
    }

    private void checkNotMapped() {
        if (Objects.equals(getHandle(), mapped.get())) {
            throw new IllegalStateException("Buffer already mapped.");
        }
    }

    public ByteBuffer mapReadOnly() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            buffer = ARBBufferObject.glMapBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, ARBPixelBufferObject.GL_READ_ONLY_ARB, size, null);
        } else {
            buffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapWriteOnly() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            buffer = ARBBufferObject.glMapBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, ARBPixelBufferObject.GL_WRITE_ONLY_ARB, size, null);
        } else {
            buffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_WRITE_ONLY, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public ByteBuffer mapReadWrite() {
        checkBound();
        checkNotMapped();
        ByteBuffer buffer;
        if (arb) {
            buffer = ARBBufferObject.glMapBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, ARBPixelBufferObject.GL_READ_WRITE_ARB, size, null);
        } else {
            buffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_WRITE, size, null);
        }
        if (buffer == null) {
            Util.checkGLError();
        }
        mapped.set(getHandle());
        return buffer;
    }

    public void unmap() {
        checkBound();
        if (!Objects.equals(mapped.get(), getHandle())) {
            throw new IllegalStateException("Buffer not mapped.");
        }
        if (arb) {
            ARBBufferObject.glUnmapBufferARB(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB);
        } else {
            GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
        }
        mapped.set(0);
    }

    public void delete() {
        if (handle != -1) {
            if (arb) {
                ARBBufferObject.glDeleteBuffersARB(getHandle());
            } else {
                GL15.glDeleteBuffers(getHandle());
            }
            handle = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (handle != -1) {
            LogManager.getLogger().warn("PBO garbage collected before deleted!");
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    delete();
                }
            });
        }
    }
}
