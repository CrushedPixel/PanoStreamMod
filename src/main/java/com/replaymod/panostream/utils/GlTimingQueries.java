package com.replaymod.panostream.utils;

import de.johni0702.minecraft.gui.utils.Consumer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

import java.util.ArrayDeque;
import java.util.Queue;

public class GlTimingQueries {
    public static final boolean SUPPORTED = GLContext.getCapabilities().OpenGL33;

    private long cpuNanosStart = 0;
    private Queue<Integer> freeQueryIds = new ArrayDeque<>();
    private Queue<Integer> scheduledQueryIds = new ArrayDeque<>();
    private Consumer<Integer> cpuConsumer;
    private Consumer<Integer> gpuConsumer;

    public GlTimingQueries(Consumer<Integer> cpuConsumer, Consumer<Integer> gpuConsumer) {
        this.cpuConsumer = cpuConsumer;
        this.gpuConsumer = gpuConsumer;
    }

    public void begin() {
        if (SUPPORTED) {
            int query;
            if (freeQueryIds.isEmpty()) {
                query = GL15.glGenQueries();
            } else {
                query = freeQueryIds.poll();
            }
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
            scheduledQueryIds.offer(query);
        }

        cpuNanosStart = System.nanoTime();
    }

    public void end() {
        cpuConsumer.consume((int) (System.nanoTime() - cpuNanosStart));
        if (SUPPORTED) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);

            while (!scheduledQueryIds.isEmpty()) {
                int query = scheduledQueryIds.peek();
                if (GL15.glGetQueryObjectui(query, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
                    gpuConsumer.consume(GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT));
                    freeQueryIds.offer(scheduledQueryIds.poll());
                } else {
                    break;
                }
            }
        }
    }

    public void destroy() {
        while (!scheduledQueryIds.isEmpty()) {
            GL15.glDeleteQueries(scheduledQueryIds.poll());
        }
        while (!freeQueryIds.isEmpty()) {
            GL15.glDeleteQueries(freeQueryIds.poll());
        }
    }
}
