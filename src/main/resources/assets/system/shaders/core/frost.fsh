#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 local;
in vec4 vertexColor;
in vec2 size;
in float radius;
in float opacity;
in float spread;

out vec4 fragColor;

const int SAMPLES = 48;
const float GOLDEN = 2.39996323;

float box(vec2 point, vec2 extent, float corner) {
    corner = min(corner, min(extent.x, extent.y));
    vec2 q = abs(point) - extent + corner;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - corner;
}

void main() {
    vec2 extent = size * 0.5;
    float dist = box(local - extent, extent, radius);
    float aa = max(fwidth(dist), 1.0e-4);
    float alpha = clamp(0.5 - dist / aa, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 base = gl_FragCoord.xy * texel;
    float reach = max(spread, 1.0);

    vec3 sum = texture(Sampler0, base).rgb;
    float total = 1.0;

    for (int i = 0; i < SAMPLES; i++) {
        float index = float(i) + 0.5;
        float ring = sqrt(index / float(SAMPLES));
        float angle = index * GOLDEN;

        vec2 offset = vec2(cos(angle), sin(angle)) * ring * reach * texel;
        float weight = 1.0 - ring * 0.4;

        sum += texture(Sampler0, base + offset).rgb * weight;
        total += weight;
    }

    vec3 blurred = sum / total;
    fragColor = vec4(mix(blurred, vertexColor.rgb, vertexColor.a), alpha * clamp(opacity, 0.0, 1.0)) * ColorModulator;
}
