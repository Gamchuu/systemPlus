#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 local;
in vec4 vertexColor;
in vec2 size;
in float radius;
in float thickness;

out vec4 fragColor;

float box(vec2 point, vec2 extent, float corner) {
    corner = min(corner, min(extent.x, extent.y));
    vec2 q = abs(point) - extent + corner;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - corner;
}

void main() {
    vec2 extent = size * 0.5;
    float dist = box(local - extent, extent, radius);

    if (thickness > 0.0) {
        dist = abs(dist + thickness * 0.5) - thickness * 0.5;
    }

    float aa = max(fwidth(dist), 1.0e-4);
    float alpha = clamp(0.5 - dist / aa, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
