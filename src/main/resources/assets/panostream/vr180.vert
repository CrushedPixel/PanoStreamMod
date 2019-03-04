#version 130

#ifdef GS
out vec4 vertColorV;
out vec2 textureCoordV;
out vec2 lightMapCoordV;
flat out mat4 projectionMatrix;
#else
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;
mat4 projectionMatrix;
#endif

uniform bool leftEye;
uniform bool overlay;
uniform float ipd;
uniform float thetaFactor;
uniform float phiFactor;
uniform float zedFactor;

vec4 transformPos(vec4 pos) {
    // Flip space to make forward be towards positive z
    pos.z *= -1.0;

    // Distort for VR180 (dark magic)
    float r = length(pos.xyz);
    vec3 ray = pos.xyz / r;
    float theta = atan(ray.x, ray.z);
    // If the vertex is far enough behind the camera, leave it there as otherwise its primitive might end up being
    // stretched over the whole screen (if the other vertex is also behind us but on a different side).
    if (abs(theta) < 2.5) {
        float phi = asin(ray.y);

        vec3 newRay = vec3(theta * thetaFactor, phi * phiFactor, zedFactor);
        newRay = normalize(newRay) * r;

        pos = vec4(newRay, 1.0);
    }

    // Flip space back to OpenGL convention (negative z is forward)
    pos.z *= -1.0;

    // Transform to screen space
    pos = projectionMatrix * pos;

    return pos;
}

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

    #ifndef GS
    pos = transformPos(pos);
    #endif

    gl_Position = pos;

    // Misc.
    #ifdef GS
    textureCoordV = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightMapCoordV = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vertColorV = gl_Color;
    #else
    textureCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightMapCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vertColor = gl_Color;
    #endif
    gl_FogFragCoord = sqrt(pos.x * pos.x + pos.y * pos.y + pos.z * pos.z);
}
