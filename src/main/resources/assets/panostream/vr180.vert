#version 110

varying vec4 vertColor;
varying vec4 textureCoord;
varying vec4 lightMapCoord;

uniform float thetaFactor;
uniform float phiFactor;
uniform float zedFactor;

void main() {
    // Transform to view space
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;

    // Distort for VR180 (dark magic)
    float r = length(pos.xyz);
    vec3 ray = pos.xyz / r;
    float theta = atan(ray.x, ray.z);
    float phi = asin(ray.y);

    vec3 newRay = vec3(theta * thetaFactor, phi * phiFactor, zedFactor);
    newRay = normalize(newRay) * r;

    pos = vec4(newRay, 1.0);

    // Transform to screen space
    gl_Position = gl_ProjectionMatrix * pos;

    // Misc.
    textureCoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
    lightMapCoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;
    vertColor = gl_Color;
    gl_FogFragCoord = sqrt(pos.x * pos.x + pos.y * pos.y + pos.z * pos.z);
}
