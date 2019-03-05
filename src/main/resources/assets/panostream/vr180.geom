#version 150

layout(lines_adjacency) in;
// max_verticies = 4 (without subdivide) + N * (N * 2 + 2) (for subdivide) where N = 10
layout(triangle_strip, max_vertices=224) out;

in vec4 vertColorV[4];
in vec2 textureCoordV[4];
in vec2 lightMapCoordV[4];
flat in mat4 projectionMatrix[];
out vec4 vertColor;
out vec2 textureCoord;
out vec2 lightMapCoord;

uniform float thetaFactor;
uniform float phiFactor;
uniform float zedFactor;

struct Vert {
    vec4 pos;
    vec4 color;
    vec2 textureCoord;
    vec2 lightMapCoord;
};

Vert mixVert(Vert v1, Vert v2, int p, int t) {
    float f = float(p) / float(t);
    return Vert(
        mix(v1.pos, v2.pos, f),
        mix(v1.color, v2.color, f),
        mix(v1.textureCoord, v2.textureCoord, f),
        mix(v1.lightMapCoord, v2.lightMapCoord, f)
    );
}

Vert in_vert(int idx) {
    return Vert(
        gl_in[idx].gl_Position,
        vertColorV[idx],
        textureCoordV[idx],
        lightMapCoordV[idx]
    );
}

vec4 transformPos(vec4 pos) {
    // Flip space to make forward be towards positive z
    pos.z *= -1.0;

    // Distort for VR180 (dark magic)
    float r = length(pos.xyz);
    vec3 ray = pos.xyz / r;
    float theta = atan(ray.x, ray.z);
    float phi = asin(ray.y);

    vec3 newRay = vec3(theta * thetaFactor, phi * phiFactor, zedFactor);
    newRay = normalize(newRay) * r;

    pos = vec4(newRay, 1.0);

    // Flip space back to OpenGL convention (negative z is forward)
    pos.z *= -1.0;

    // Transform to screen space
    pos = projectionMatrix[0] * pos;

    return pos;
}

Vert transformVertex(Vert vert) {
    vert.pos = transformPos(vert.pos);
    return vert;
}

void emitVertex(Vert vert) {
    gl_Position = vert.pos;
    textureCoord = vert.textureCoord;
    lightMapCoord = vert.lightMapCoord;
    vertColor = vert.color;
    EmitVertex();
}

void subdivide(Vert v0, Vert v1, Vert v2, Vert v3) {
    const int N = 10;
    for (int x = 0; x < N; x++) {
        Vert lt = mixVert(v0, v3, x, N);
        Vert lb = mixVert(v0, v3, x+1, N);
        Vert rt = mixVert(v1, v2, x, N);
        Vert rb = mixVert(v1, v2, x+1, N);
        emitVertex(transformVertex(lb));
        for (int y = 0; y < N; y++) {
            emitVertex(transformVertex(mixVert(lt, rt, y, N)));
            emitVertex(transformVertex(mixVert(lb, rb, y+1, N)));
        }
        emitVertex(transformVertex(rt));
        EndPrimitive();
    }
}

void main() {
    Vert v0 = in_vert(0);
    Vert v1 = in_vert(1);
    Vert v2 = in_vert(2);
    Vert v3 = in_vert(3);

    #ifdef OVERLAY

    subdivide(v0, v1, v2, v3);

    #else

    // Clip any objects behind the player
    if (v0.pos.z > 0 && v1.pos.z > 0 && v2.pos.z > 0 && v3.pos.z > 0) {
        float sx = sign(v0.pos.x);
        // That would end up with verticies on different sides after the 180 transformation
        if (sx != sign(v1.pos.x) || sx != sign(v2.pos.x) || sx != sign(v3.pos.x)) {
            return;
        }
    }

    vec4 s0 = v0.pos - v1.pos;
    vec4 s1 = v1.pos - v2.pos;
    vec4 s2 = v2.pos - v3.pos;
    vec4 s3 = v3.pos - v0.pos;
    float maxSideLen = max(
        max(dot(s0, s0), dot(s1, s1)),
        max(dot(s2, s2), dot(s3, s3))
    );
    float d0 = dot(v0.pos, v0.pos) / maxSideLen;
    float d1 = dot(v1.pos, v1.pos) / maxSideLen;
    float d2 = dot(v2.pos, v2.pos) / maxSideLen;
    float d3 = dot(v3.pos, v3.pos) / maxSideLen;
    float d = min(min(d0, d1), min(d2, d3));
    if (d < 25) {
        subdivide(v0, v1, v2, v3);
    }
    if (d > 9) {
        emitVertex(transformVertex(v3));
        emitVertex(transformVertex(v0));
        emitVertex(transformVertex(v2));
        emitVertex(transformVertex(v1));
        EndPrimitive();
    }

    #endif
}
