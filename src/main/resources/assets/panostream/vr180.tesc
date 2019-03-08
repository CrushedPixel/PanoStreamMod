#version 400

#ifdef NO_TESSELLATION
#error "Cannot use NO_TESSELLATION with TCS"
#endif

#ifdef DRAW_INSTANCED
#define renderPass renderPassV[gl_InvocationID]
flat in int renderPassV[];
out int renderPassC[];
#endif

#define NO_SINGLE_PASS_PROJECTION
#include vr180.glsl

layout(vertices = 4) out;

in vec4 vertColorV[];
in vec2 textureCoordV[];
in vec2 lightMapCoordV[];
flat in mat4 projectionMatrix[];
out vec4 vertColorC[];
out vec2 textureCoordC[];
out vec2 lightMapCoordC[];
out mat4 projectionMatrixC[];

void main() {
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
    vertColorC[gl_InvocationID] = vertColorV[gl_InvocationID];
    textureCoordC[gl_InvocationID] = textureCoordV[gl_InvocationID];
    lightMapCoordC[gl_InvocationID] = lightMapCoordV[gl_InvocationID];
    projectionMatrixC[gl_InvocationID] = projectionMatrix[gl_InvocationID];
    #ifdef DRAW_INSTANCED
    renderPassC[gl_InvocationID] = renderPass;
    #endif

    float level;
    if (IS_PASS_MC) {
        level = 1.0;
    } else {
        // Tessellation level based on apparent size after vr180 transformation (only an approximation since we'd
        // have to do tessellation to get the actual apparent size which includes any curving).
        vec4 p = vr180Projection(projectionMatrix[0], gl_in[gl_InvocationID].gl_Position);
        vec4 q = vr180Projection(projectionMatrix[0], gl_in[(gl_InvocationID + 1) % 4].gl_Position);
        p /= p.w;
        q /= q.w;
        float apparentSize = distance(p.xy, q.xy);
        level = clamp(MAX_TESS_LEVEL * apparentSize, 1.0, MAX_TESS_LEVEL);
    }

    // Need to use if-cascade here because otherwise I seem to be hitting
    // https://computergraphics.stackexchange.com/questions/1729/
    if (gl_InvocationID == 0) {
        gl_TessLevelOuter[gl_InvocationID] = level;
    } else if (gl_InvocationID == 1) {
        gl_TessLevelOuter[gl_InvocationID] = level;
    } else if (gl_InvocationID == 2) {
        gl_TessLevelOuter[gl_InvocationID] = level;
    } else if (gl_InvocationID == 3) {
        gl_TessLevelOuter[gl_InvocationID] = level;
    }

    barrier();

    float otherLevel = gl_TessLevelOuter[(gl_InvocationID + 2) % 4];
    float innerLevel = max(level, otherLevel);
    if (gl_InvocationID % 2 == 1) {
        gl_TessLevelInner[0] = innerLevel;
    } else {
        gl_TessLevelInner[1] = innerLevel;
    }
}
