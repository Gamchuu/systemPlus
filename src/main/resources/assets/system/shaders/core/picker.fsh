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

float boxDistance(vec2 point, vec2 halfSize, float radius) {
    radius = min(radius, min(halfSize.x, halfSize.y));

    vec2 offset = abs(point) - halfSize + radius;
    return min(max(offset.x, offset.y), 0.0) + length(max(offset, 0.0)) - radius;
}

vec3 wheel(float hue) {
    vec3 wedge = fract(hue + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0));

    return clamp(abs(wedge * 6.0 - 3.0) - 1.0, 0.0, 1.0);
}

float edge(float dist) {
    float aa = max(fwidth(dist), 1.0e-4);
    return clamp(0.5 - dist / aa, 0.0, 1.0);
}

void main() {
    vec2 halfSize = quadSize * 0.5;
    vec2 point = (texCoord0 - 0.5) * quadSize;

    float dist = boxDistance(point, halfSize, corners.x);
    float alpha = edge(dist);

    if (alpha <= 0.001) discard;

    vec3 shade;

    if (corners.y < 0.5) {
        shade = mix(vec3(1.0), wheel(extra / 100.0), texCoord0.x) * (1.0 - texCoord0.y);
    } else {
        shade = wheel(texCoord0.y);
    }

    fragColor = vec4(shade, alpha) * vertexColor * ColorModulator;
}
