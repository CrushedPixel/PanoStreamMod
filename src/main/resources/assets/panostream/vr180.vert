#version 130

#include vr180.glsl

#ifdef WITH_GS
out vec4 vertColorV;
out vec2 textureCoordV;
out vec2 lightMapCoordV;
flat out mat4 projectionMatrix;
#else
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;
#endif

#ifdef OVERLAY
const float n = 0.01; // near
const float f = 2.0; // far
const float l = n; // left; take up 90 degree of FOV
const float t = l; // top; in either direction
const mat4 overlayProjectionMatrix = mat4(
    vec4(n/l, 0.0, 0.0, 0.0),
    vec4(0.0, n/t, 0.0, 0.0),
    vec4(0.0, 0.0, -(f+n)/(f-n), -1.0),
    vec4(0.0, 0.0, -2.0*f*n/(f-n), 0.0)
);
#endif

void main() {
    // Transform to view space
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;

    #ifdef OVERLAY
    // When rendering with orthographic projection, immediately apply the projection matrix
    pos = gl_ProjectionMatrix * pos;
    pos /= pos.w;
    // and position the result (NDC) such that (0, 0, 0) is right in front of the camera at one block distance
    pos.z *= 0.01; // but almost flat
    pos.z *= -1.0; // forward for NDC is towards positive but per OpenGL convention it should be towards negative
    pos.z += -1.0; // at one block distance
    // finally, continue as if we were doing perspective projection all along
    #endif

    // Offset for stereoscopy
    #ifndef SINGLE_PASS_WITH_GS_INSTANCING
    if (leftEye) {
        pos.x -= ipd * 0.5;
    } else {
        pos.x += ipd * 0.5;
    }
    #endif

    #ifdef WITH_GS
    #ifdef OVERLAY
    projectionMatrix = overlayProjectionMatrix;
    #else
    projectionMatrix = gl_ProjectionMatrix;
    #endif
    gl_Position = pos;
    #else
    #ifdef OVERLAY
    gl_Position = vr180Projection(overlayProjectionMatrix, pos);
    #else
    gl_Position = vr180Projection(gl_ProjectionMatrix, pos);
    #endif
    #endif

    // Misc.
    #ifdef WITH_GS
    textureCoordV = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightMapCoordV = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vertColorV = gl_Color;
    #else
    textureCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightMapCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vertColor = gl_Color;
    #endif
    gl_FogFragCoord = length(pos.xyz);
}
