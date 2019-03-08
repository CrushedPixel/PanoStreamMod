#version 150

#if defined(DRAW_INSTANCED) && !defined(WITH_TESS)
flat in int renderPassV[];
#define renderPass renderPassV[0]
#endif

#include vr180.glsl

#ifdef GS_INSTANCING
    #ifdef ZERO_PASS
        #define INVOCATIONS 3
    #else
        #define INVOCATIONS 2
    #endif
#else
    #define INVOCATIONS 1
#endif

#ifdef WITH_TES
    // When using TES, tessellation has already been taken care of, we're just using instancing for stereoscopy
    #define NO_TESSELLATION
    #define IN_VERTS 3
    #define MAX_OUT_VERTS 3 // merely using instancing for stereoscopy
    #define PRIMITIVE triangles
#else
    #define IN_VERTS 4
    #ifdef NO_TESSELLATION
        #define MAX_OUT_VERTS 4
    #else
        // max_verticies for overlay is N * (N * 2 + 2) = 220 where N = 10
        // max_verticies for world is 4 (without subdivide) + N * (N * 2 + 2) (for subdivide) = 224 where N = 10
        // we output 4 + 4 + 2 + 2 + 1 = 13 components per vertex
        #define MAX_OUT_VERTS 224
    #endif
    #define PRIMITIVE lines_adjacency
#endif

layout(PRIMITIVE, invocations=INVOCATIONS) in;
layout(triangle_strip, max_vertices=MAX_OUT_VERTS) out;

in vec4 vertColorV[IN_VERTS];
in vec2 textureCoordV[IN_VERTS];
in vec2 lightMapCoordV[IN_VERTS];
flat in mat4 projectionMatrix[];
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;

Vert in_vert(int idx) {
    vec4 pos = gl_in[idx].gl_Position;
    #ifdef GS_INSTANCING
    // Offset for stereoscopy
    if (IS_PASS_LEFT_EYE) {
        pos += vec4(ipd / 2.0, 0.0, 0.0, 0.0);
    } else if (IS_PASS_RIGHT_EYE) {
        pos -= vec4(ipd / 2.0, 0.0, 0.0, 0.0);
    }
    #endif
    return Vert(
        pos,
        vertColorV[idx],
        textureCoordV[idx],
        lightMapCoordV[idx]
    );
}

void emitVertex(Vert vert) {
    gl_Position = vr180Projection(projectionMatrix[0], vert.pos);
    textureCoord = vert.textureCoord;
    lightMapCoord = vert.lightMapCoord;
    vertColor = vert.color;
    EmitVertex();
}

void subdivide(Vert v0, Vert v1, Vert v2, Vert v3, int tx, int ty) {
    for (int x = 0; x < tx; x++) {
        Vert lt = mixVert(v0, v3, x, tx);
        Vert lb = mixVert(v0, v3, x+1, tx);
        Vert rt = mixVert(v1, v2, x, tx);
        Vert rb = mixVert(v1, v2, x+1, tx);
        emitVertex(lb);
        for (int y = 0; y < ty; y++) {
            emitVertex(mixVert(lt, rt, y, ty));
            emitVertex(mixVert(lb, rb, y+1, ty));
        }
        emitVertex(rt);
        EndPrimitive();
    }
}

void main() {
    #ifdef GS_INSTANCING
    renderPass = gl_InvocationID;
    #endif

    Vert v0 = in_vert(0);
    Vert v1 = in_vert(1);
    Vert v2 = in_vert(2);
    #if IN_VERTS > 3
    Vert v3 = in_vert(3);
    #endif

    #ifdef NO_TESSELLATION

    #if IN_VERTS == 3
    emitVertex(v0);
    emitVertex(v1);
    emitVertex(v2);
    #else
    emitVertex(v3);
    emitVertex(v0);
    emitVertex(v2);
    emitVertex(v1);
    #endif
    EndPrimitive();

    #else

    if (IS_PASS_MC) {
        emitVertex(v3);
        emitVertex(v0);
        emitVertex(v2);
        emitVertex(v1);
        EndPrimitive();
        return;
    }

    #ifdef OVERLAY

    // When rendering the overlay, 99% of inputs are flat rectangles, so it makes lots of sense to
    // tessellate different amounts in different directions.
    // The background quad of a GuiScreen will have lengths 1 (full virtual screen) can be be subdivided into
    // at most 10x10 smaller quads.
    int tx = clamp(int(length(v0.pos - v3.pos) * 10.0), 1, 10);
    int ty = clamp(int(length(v0.pos - v1.pos) * 10.0), 1, 10);
    if (tx > 1 || ty > 1) {
        subdivide(v0, v1, v2, v3, tx, ty);
    } else {
        emitVertex(v3);
        emitVertex(v0);
        emitVertex(v2);
        emitVertex(v1);
        EndPrimitive();
    }

    #else

    // Clip any objects behind the player
    if (v0.pos.z > 0 && v1.pos.z > 0 && v2.pos.z > 0 && v3.pos.z > 0) {
        float sx = sign(v0.pos.x);
        // That would end up with verticies on different sides after the 180 transformation
        if (sx != sign(v1.pos.x) || sx != sign(v2.pos.x) || sx != sign(v3.pos.x)) {
            return;
        }
    }

    vec4 s0 = v0.pos - v1.pos;
    vec4 s1 = v1.pos - v2.pos;
    vec4 s2 = v2.pos - v3.pos;
    vec4 s3 = v3.pos - v0.pos;
    float maxSideLen = max(
        max(dot(s0, s0), dot(s1, s1)),
        max(dot(s2, s2), dot(s3, s3))
    );
    float d0 = dot(v0.pos, v0.pos) / maxSideLen;
    float d1 = dot(v1.pos, v1.pos) / maxSideLen;
    float d2 = dot(v2.pos, v2.pos) / maxSideLen;
    float d3 = dot(v3.pos, v3.pos) / maxSideLen;
    float d = min(min(d0, d1), min(d2, d3));
    if (d < 25) {
        const int N = 10;
        subdivide(v0, v1, v2, v3, N, N);
    }
    if (d > 9) {
        emitVertex(v3);
        emitVertex(v0);
        emitVertex(v2);
        emitVertex(v1);
        EndPrimitive();
    }

    #endif
    #endif
}
