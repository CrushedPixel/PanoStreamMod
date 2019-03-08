#version 110

// Emulates MC's lighting,
// which is lost when not using the fixed function OpenGL pipeline.
// Originally written by johni0702 for the Minecraft ReplayMod.

varying vec4 vertColor;
varying vec2 textureCoord;
varying vec2 lightMapCoord;
varying float fogDist;

uniform sampler2D texture;
uniform sampler2D lightMap;

uniform bool textureEnabled;
uniform bool lightMapEnabled;
uniform bool hurtTextureEnabled;
uniform bool fogEnabled;

void main() {
    vec4 color = vertColor;

    if (textureEnabled) {
        color *= texture2D(texture, textureCoord);
    }

    if (lightMapEnabled) {
        color *= texture2D(lightMap, lightMapCoord);
    }

    if (hurtTextureEnabled) {
        color = vec4(mix(color.rgb, vec3(1, 0, 0), 0.3), color.a);
    }

    if (fogEnabled) {
        color.rgb = mix(color.rgb, gl_Fog.color.rgb, clamp((fogDist - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
    }

    gl_FragColor = color;
}