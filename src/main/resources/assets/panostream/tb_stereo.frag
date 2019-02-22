#version 110

// composes two textures into a single top-bottom stereo projection.

// the two input textures
uniform sampler2D leftEyeTex;
uniform sampler2D rightEyeTex;

// whether to flip the image
uniform int flip;

void main() {
	// normalized texture coordinates (from 0 to 1)
    float u = gl_TexCoord[0].x;
    float v = gl_TexCoord[0].y;

    // vertically flip the image if needed
    if (flip == 1) {
        v = 1.0 - v;
    }

    if (v < 0.5) {
        gl_FragColor = texture2D(leftEyeTex, vec2(u, v * 2.0));
    } else {
        gl_FragColor = texture2D(rightEyeTex, vec2(u, (v - 0.5) * 2.0));
    }
}
