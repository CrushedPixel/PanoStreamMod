#version 150 compatibility

#ifdef DRAW_INSTANCED
int renderPass;
#endif

#include vr180.glsl

#ifdef WITH_INTERMEDIATE
out vec4 vertColorV;
out vec2 textureCoordV;
out vec2 lightMapCoordV;
flat out mat4 projectionMatrix;
#ifdef DRAW_INSTANCED
flat out int renderPassV;
#endif
#else
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;
mat4 projectionMatrix;
#endif

#ifdef OVERLAY
const float overlayNear = 0.01; // near
const float overlayFar = 2.0; // far
const float overlayLeft = overlayNear; // left; take up 90 degree of FOV
const float overlayTop = overlayLeft; // top; in either direction
const mat4 overlayProjectionMatrix = mat4(
    vec4(overlayNear/overlayLeft, 0.0, 0.0, 0.0),
    vec4(0.0, overlayNear/overlayTop, 0.0, 0.0),
    vec4(0.0, 0.0, -(overlayFar+overlayNear)/(overlayFar-overlayNear), -1.0),
    vec4(0.0, 0.0, -2.0*overlayFar*overlayNear/(overlayFar-overlayNear), 0.0)
);
#endif
const float worldNear = 0.05; // near
const float worldFar = FAR_PLANE_DISTANCE; // far
const float worldLeft = worldNear; // left; take up 90 degree of FOV
const float worldTop = worldLeft; // top; in either direction
const mat4 worldProjectionMatrix = mat4(
vec4(worldNear/worldLeft, 0.0, 0.0, 0.0),
vec4(0.0, worldNear/worldTop, 0.0, 0.0),
vec4(0.0, 0.0, -(worldFar+worldNear)/(worldFar-worldNear), -1.0),
vec4(0.0, 0.0, -2.0*worldFar*worldNear/(worldFar-worldNear), 0.0)
);

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
    #ifdef DRAW_INSTANCED
    renderPass = gl_InstanceID;
    #endif
    if (IS_PASS_LEFT_EYE) {
        pos.x += ipd * 0.5;
    } else if (IS_PASS_RIGHT_EYE) {
        pos.x -= ipd * 0.5;
    }
    #endif

    if (IS_PASS_MC) {
        projectionMatrix = gl_ProjectionMatrix;
    } else {
        #ifdef OVERLAY
        projectionMatrix = overlayProjectionMatrix;
        #else
        projectionMatrix = worldProjectionMatrix;
        #endif
    }

    #ifdef WITH_INTERMEDIATE
    gl_Position = pos;
    #else
    gl_Position = vr180Projection(projectionMatrix, pos);
    #endif

    // Misc.
    #ifdef WITH_INTERMEDIATE
    #ifdef DRAW_INSTANCED
    renderPassV = renderPass;
    #endif
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
