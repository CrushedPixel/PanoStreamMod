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

    bool leftEye = v < 0.5;
    if (!leftEye) {
        v -= 0.5;
    }

    // FIXME get rid of this shader in favor of glBlitFramebuffer

    v *= 2.0;
    v = 1.0 - v;

    if (leftEye) {
        gl_FragColor = texture2D(leftEyeTex, vec2(u, v));
    } else {
        gl_FragColor = texture2D(rightEyeTex, vec2(u, v));
    }
}
