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

const int SAMPLES = 48;
const float GOLDEN = 2.39996323;

float boxDistance(vec2 point, vec2 halfSize, vec4 radii) {
    float radius = (point.x > 0.0)
        ? ((point.y > 0.0) ? radii.z : radii.w)
        : ((point.y > 0.0) ? radii.y : radii.x);

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

    float alpha = edge(boxDistance(point, halfSize, vec4(corners.x)));
    if (alpha <= 0.001) discard;

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 base = gl_FragCoord.xy * texel;
    float radius = max(extra, 1.0);

    vec3 sum = texture(Sampler0, base).rgb;
    float total = 1.0;

    for (int i = 0; i < SAMPLES; i++) {
        float index = float(i) + 0.5;
        float spread = sqrt(index / float(SAMPLES));
        float angle = index * GOLDEN;

        vec2 offset = vec2(cos(angle), sin(angle)) * spread * radius * texel;
        float weight = 1.0 - spread * 0.4;

        sum += texture(Sampler0, base + offset).rgb * weight;
        total += weight;
    }

    vec3 blurred = sum / total;
    fragColor = vec4(mix(blurred, vertexColor.rgb, vertexColor.a), alpha * clamp(corners.w / 100.0, 0.0, 1.0)) * ColorModulator;
}
