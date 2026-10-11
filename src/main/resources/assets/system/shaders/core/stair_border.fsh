#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};



in vec2 texCoord0;
in vec4 vertexColor;
in vec2 quadSize;
in vec4 corners;
in float extra;

out vec4 fragColor;


float boxAt(vec2 point, float left, float top, float width, float height, vec4 radii) {
    vec2 centre = vec2(left + width * 0.5, top + height * 0.5);
    vec2 halfSize = vec2(width * 0.5, height * 0.5);
    vec2 local = point - centre;

    float radius = (local.x > 0.0)
        ? ((local.y > 0.0) ? radii.z : radii.w)
        : ((local.y > 0.0) ? radii.y : radii.x);

    radius = min(radius, min(halfSize.x, halfSize.y));

    vec2 offset = abs(local) - halfSize + radius;
    return min(max(offset.x, offset.y), 0.0) + length(max(offset, 0.0)) - radius;
}

float stairDistance(vec2 point, vec2 size, vec4 corners) {
    float bend = corners.z;
    float own = size.x - bend;
    float tall = size.y;
    float above = corners.x;
    float below = corners.y;

    float topLeft = above > 0.5 ? 0.0 : bend;
    float topRight = above > 0.5 ? 0.0 : bend;
    float bottomLeft = below > 0.5 ? 0.0 : bend;

    float dist = boxAt(point, 0.0, 0.0, own, tall, vec4(topLeft, bottomLeft, bend, topRight));

    if (above > 0.5)
        dist = min(dist, boxAt(point, 0.0, -tall, min(above, own), tall * 2.0,
            vec4(0.0, bottomLeft, bend, 0.0)));

    if (below > 0.5)
        dist = min(dist, boxAt(point, 0.0, 0.0, min(below, own), tall * 2.0,
            vec4(topLeft, 0.0, 0.0, topRight)));

    return dist;
}

float edge(float dist) {
    float aa = max(fwidth(dist), 1.0e-4);
    return clamp(0.5 - dist / aa, 0.0, 1.0);
}

void main() {
    vec2 point = texCoord0 * quadSize;

    float dist = stairDistance(point, quadSize, corners);
    float thickness = max(extra, 1.0);

    float outside = edge(dist);
    float inside = edge(dist + thickness);
    float alpha = clamp(outside - inside, 0.0, 1.0);

    if (alpha <= 0.001) discard;

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
