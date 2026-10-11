#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;
in vec2 quadSize;
in vec4 corners;
in float extra;

out vec4 fragColor;

float boxDistance(vec2 point, vec2 halfSize, float radius) {
    radius = min(radius, min(halfSize.x, halfSize.y));

    vec2 offset = abs(point) - halfSize + radius;
    return min(max(offset.x, offset.y), 0.0) + length(max(offset, 0.0)) - radius;
}

float edge(float dist) {
    float aa = max(fwidth(dist), 1.0e-4);
    return clamp(0.5 - dist / aa, 0.0, 1.0);
}

void main() {
    vec2 halfSize = quadSize * 0.5;
    vec2 point = (texCoord0 - 0.5) * quadSize;

    float alpha = edge(boxDistance(point, halfSize, extra));
    if (alpha <= 0.001) discard;

    vec2 uv = (corners.xy + clamp(texCoord0, 0.0, 1.0) * corners.zw) / 64.0;
    vec4 sampled = texture(Sampler0, uv) * vertexColor;
    if (sampled.a <= 0.001) discard;

    fragColor = vec4(sampled.rgb, sampled.a * alpha) * ColorModulator;
}
