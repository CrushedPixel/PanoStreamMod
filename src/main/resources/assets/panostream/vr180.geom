#version 330 core

layout(triangles) in;
layout(triangle_strip, max_vertices=3) out;

void emitVertex(vec3 vec) {
    // TODO: displace vertex by formula and then emit it
}

void emitTriangle(vec3 v1, vec3 v2, vec3 v3) {
    emitVertex(v1);
    emitVertex(v2);
    emitVertex(v3);

    // TODO: end primitive
}

vec3 middle(vec3 v1, vec3 v2) {
    // Calculate middle
    vec3 v12 = vec3(0.5f * (v1.x + v2.x), 0.5f * (v1.y + v2.y), 0.5f * (v1.z + v2.z));

    // renormalize
    float s = 1.0f / sqrt(v12.x * v12.x + v12.y * v12.y + v12.z * v12.z);
    v12 *= s;

    return v12;
}

void subdivide(vec3 v1, vec3 v2, vec3 v3,
               int level) {
    if (level == 0) {
        // Reached desired tessellation level, emit triangle.
        emitTriangle(v1, v2, v3);

    } else {
        vec3 v12 = middle(v1, v2);
        vec3 v13 = middle(v1, v3);
        vec3 v23 = middle(v2, v3);

        // Make the recursive calls.
        subdivide(v1, v12, v13, level - 1);
        subdivide(v12, v2, v23, level - 1);
        subdivide(v13, v23, v3, level - 1);
        subdivide(v12, v23, v13, level - 1);
    }
}

void main() {

}