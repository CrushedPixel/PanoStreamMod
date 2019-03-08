#if defined(DRAW_INSTANCED) && defined(GS_INSTANCING)
#undef GS_INSTANCING
#endif

#if defined(SINGLE_PASS) && defined(ZERO_PASS)
#error "SINGLE_PASS and ZERO_PASS must not both be active"
#endif

#if defined(DRAW_INSTANCED) && !defined(SINGLE_PASS) && !defined(ZERO_PASS)
#error "DRAW_INSTANCED requires SINGLE_PASS or ZERO_PASS"
#endif

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

#define PASS_LEFT_EYE 0
#define PASS_RIGHT_EYE 1
#define PASS_MC 2

#define IS_PASS_LEFT_EYE (renderPass == PASS_LEFT_EYE)
#define IS_PASS_RIGHT_EYE (renderPass == PASS_RIGHT_EYE)
#ifdef ZERO_PASS
#define IS_PASS_MC (renderPass == PASS_MC)
#else
#define IS_PASS_MC false
#endif

#ifdef SINGLE_PASS_WITH_GS_INSTANCING
int renderPass;
#else
#ifndef DRAW_INSTANCED
uniform int renderPass;
#endif
#endif
uniform float ipd;
uniform float thetaFactor;
uniform float phiFactor;
uniform float zedFactor;
#ifdef ZERO_PASS
uniform float inverseViewportAspect;
uniform float mcAspect;
uniform float mcWidthFraction;
uniform float mcHeightFraction;
uniform float eyeWidthFraction;
uniform float eyeHeightFraction;
#endif

#if defined(IN_VS) && !defined(WITH_INTERMEDIATE) || defined(IN_TES) && !defined(WITH_GS) || defined(IN_GS)
out float fogDist;
#else
#define NO_FOG
#endif

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
    if (!IS_PASS_MC) {
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
    }

    #ifndef NO_FOG
    // Set fog distance
    fogDist = length(pos.xyz);
    #endif

    // Transform to screen space
    pos = projectionMatrix * pos;

    #ifndef NO_SINGLE_PASS_PROJECTION
    #ifdef ZERO_PASS
    // perspective divide since we want to move things around on the screen (keep w so we can undo it later)
    pos.xyz /= pos.w;
    // flip up-side-down (in multi-pass mode this is done by the composition shader)
    pos.y *= -1.0;
    // scale the whole viewport [-1; 1] to [-0.5; 0.5] which we can more intuitively work with it.
    pos.xy *= 0.5;
    // de-normalize viewport ratio from [-0.5; 0.5] to [-?; ?] in x direction (height preserved)
    // (i.e. if the resulting frame would be rendered as is, it would no longer be stretched in either direction)
    pos.x *= inverseViewportAspect;
    if (IS_PASS_MC) {
        // scale to MC size
        pos.xy *= mcHeightFraction;
        pos.x *= mcAspect;
        // clip primitives
        gl_ClipDistance[0] = pos.y + mcHeightFraction * 0.5; // above
        gl_ClipDistance[1] = 0.0; // below (already at bottom)
        // Move  bottom-left of MC window to bottom-left of screen
        pos.x +=  mcWidthFraction * 0.5      - 0.5;
        pos.y += -mcHeightFraction * 0.5     + 0.5;
    } else {
        // scale to eye size
        pos.xy *= eyeHeightFraction;
        // clip primitives
        gl_ClipDistance[0] = pos.y + eyeHeightFraction * 0.5; // above
        gl_ClipDistance[1] = eyeHeightFraction * 0.5 - pos.y; // below
        // Move  top-left of eye to        top-left of screen
        pos.x += eyeWidthFraction * 0.5  - 0.5;
        pos.y += eyeHeightFraction * 0.5 - 0.5;
        if (IS_PASS_RIGHT_EYE) {
            // Move to right eye (i.e. one frame down)
            pos.y += eyeHeightFraction;
        }
    }
    // scale [-0.5, 0.5] back to the whole viewport [-1; 1]
    pos.xy *= 2;
    // undo perspective divide so we get perspective-correct interpolation
    pos.xyz *= pos.w;
    gl_ClipDistance[0] *= pos.w;
    gl_ClipDistance[1] *= pos.w;
    #endif

    #ifdef SINGLE_PASS
    // flip up-side-down (in multi-pass mode this is done by the composition shader)
    pos.y *= -1.0;
    // squash whole viewport [-1; 1] down to the height of a single eye [-0.5, 0.5]
    pos.y *= 0.5;
    if (IS_PASS_LEFT_EYE) {
        // Move to left eye
        pos.y -= 0.5 * pos.w;
        // Clip any primitive reaching into the right eye
        gl_ClipDistance[0] = -pos.y;
    } else {
        // Move to right eye
        pos.y += 0.5 * pos.w;
        // Clip any primitive reaching into the left eye
        gl_ClipDistance[0] = pos.y;
    }
    #endif
    #endif

    #ifdef MARK_LEFT_EYE
    if (IS_PASS_LEFT_EYE) {
        pos.x = 0;
    }
    #endif

    return pos;
}
