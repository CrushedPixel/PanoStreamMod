#ifdef GS_INSTANCING
#ifdef WITH_GS
#define SINGLE_PASS_WITH_GS_INSTANCING
#else
#define SINGLE_PASS_WITHOUT_GS_INSTANCING
#endif
#endif

#ifdef WITH_GS
#define WITH_INTERMEDIATE
#endif

#ifdef WITH_TES
#define WITH_INTERMEDIATE
#endif

#ifdef SINGLE_PASS_WITH_GS_INSTANCING
bool leftEye;
#else
uniform bool leftEye;
#endif
uniform float ipd;
uniform float thetaFactor;
uniform float phiFactor;
uniform float zedFactor;

struct Vert {
    vec4 pos;
    vec4 color;
    vec2 textureCoord;
    vec2 lightMapCoord;
};

Vert mixVert(Vert v1, Vert v2, float f) {
    return Vert(
        mix(v1.pos, v2.pos, f),
        mix(v1.color, v2.color, f),
        mix(v1.textureCoord, v2.textureCoord, f),
        mix(v1.lightMapCoord, v2.lightMapCoord, f)
    );
}

Vert mixVert(Vert v1, Vert v2, int p, int t) {
    return mixVert(v1, v2, float(p) / float(t));
}

vec4 vr180Projection(mat4 projectionMatrix, vec4 pos) {
    // Flip space to make forward be towards positive z
    pos.z *= -1.0;

    // If the vertex is far enough behind the camera, leave it there as otherwise its primitive might end up being
    // stretched over the whole screen (if the other vertex is also behind us but on a different side).
    // This is a crude estimation for the vertex shader and requires the primitive to be sufficiently small,
    // the geometry shader (which is used for nearby/big things) has an additional exact check.
    float theta = atan(pos.x, pos.z);
    if (abs(theta) < 2.5) {
        // Distort for VR180 (dark magic)
        float r = length(pos.xyz);
        vec3 ray = pos.xyz / r;
        float phi = asin(ray.y);

        vec3 newRay = vec3(theta * thetaFactor, phi * phiFactor, zedFactor);
        newRay = normalize(newRay) * r;

        pos = vec4(newRay, 1.0);
    }

    // Flip space back to OpenGL convention (negative z is forward)
    pos.z *= -1.0;

    // Transform to screen space
    pos = projectionMatrix * pos;

    #ifndef NO_SINGLE_PASS_PROJECTION
    #ifdef SINGLE_PASS
    // flip up-side-down (in multi-pass mode this is done by the composition shader)
    pos.y *= -1.0;
    // squash whole viewport [-1; 1] down to the height of a single eye [-0.5, 0.5]
    pos.y *= 0.5;
    if (leftEye) {
        // Move to left eye
        pos.y += 0.5 * pos.w;
        // Clip any primitive reaching into the right eye
        gl_ClipDistance[0] = pos.y;
    } else {
        // Move to right eye
        pos.y -= 0.5 * pos.w;
        // Clip any primitive reaching into the left eye
        gl_ClipDistance[0] = -pos.y;
    }
    #endif
    #endif

    return pos;
}
