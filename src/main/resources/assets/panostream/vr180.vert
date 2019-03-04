#version 130

out vec4 vertColorV;
out vec2 textureCoordV;
out vec2 lightMapCoordV;
flat out mat4 projectionMatrix;

uniform bool leftEye;
uniform bool overlay;
uniform float ipd;

void main() {
    // Transform to view space
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;
    if (overlay) {
        // When rendering with orthographic projection, immediately apply the projection matrix
        pos = gl_ProjectionMatrix * pos;
        pos /= pos.w;
        // and position the result (NDC) such that (0, 0, 0) is right in front of the camera at one block distance
        pos.z *= 0.01; // but almost flat
        pos.z *= -1.0; // forward for NDC is towards positive but per OpenGL convention it should be towards negative
        pos += vec4(0.0, 0.0, -1.0, 0.0);
        // finally, pretend we were doing perspective projection all along
        float n = 0.01;
        float f = 2.0;
        float l = n; // take up 90 degree of FOV
        float t = l; // in either direction
        projectionMatrix[0] = vec4(n/l, 0.0, 0.0, 0.0);
        projectionMatrix[1] = vec4(0.0, n/t, 0.0, 0.0);
        projectionMatrix[2] = vec4(0.0, 0.0, -(f+n)/(f-n), -1.0);
        projectionMatrix[3] = vec4(0.0, 0.0, -2.0*f*n/(f-n), 0.0);
    } else {
        projectionMatrix = gl_ProjectionMatrix;
    }

    // Offset for stereoscopy
    if (leftEye) {
        pos -= vec4(ipd / 2.0, 0.0, 0.0, 0.0);
    } else {
        pos += vec4(ipd / 2.0, 0.0, 0.0, 0.0);
    }

    gl_Position = pos;

    // Misc.
    textureCoordV = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightMapCoordV = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vertColorV = gl_Color;
    gl_FogFragCoord = sqrt(pos.x * pos.x + pos.y * pos.y + pos.z * pos.z);
}
