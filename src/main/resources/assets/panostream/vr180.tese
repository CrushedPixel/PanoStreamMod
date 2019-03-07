#version 400

#include vr180.glsl

layout(quads) in;

in vec4 vertColorC[gl_MaxPatchVertices];
in vec2 textureCoordC[gl_MaxPatchVertices];
in vec2 lightMapCoordC[gl_MaxPatchVertices];
in mat4 projectionMatrixC[gl_MaxPatchVertices];
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;

Vert in_vert(int idx) {
    return Vert(
        gl_in[idx].gl_Position,
        vertColorC[idx],
        textureCoordC[idx],
        lightMapCoordC[idx]
    );
}

void main() {
    Vert v0 = in_vert(0);
    Vert v1 = in_vert(1);
    Vert v2 = in_vert(2);
    Vert v3 = in_vert(3);
    Vert vert = mixVert(mixVert(v1, v0, gl_TessCoord[1]), mixVert(v2, v3, gl_TessCoord[1]), gl_TessCoord[0]);

    gl_Position = vr180Projection(projectionMatrixC[0], vert.pos);
    textureCoord = vert.textureCoord;
    lightMapCoord = vert.lightMapCoord;
    vertColor = vert.color;
}
