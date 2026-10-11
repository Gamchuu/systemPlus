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
const vec2 LIGHT = vec2(-0.6, -0.8);

float boxDistance(vec2 point, vec2 halfSize, vec4 radii) {
    float radius = (point.x > 0.0)
        ? ((point.y > 0.0) ? radii.z : radii.w)
        : ((point.y > 0.0) ? radii.y : radii.x);

    radius = min(radius, min(halfSize.x, halfSize.y));

    vec2 offset = abs(point) - halfSize + radius;
    return min(max(offset.x, offset.y), 0.0) + length(max(offset, 0.0)) - radius;
}

vec2 boxNormal(vec2 point, vec2 halfSize, vec4 radii) {
    vec2 nudge = vec2(0.6, 0.0);

    vec2 slope = vec2(
        boxDistance(point + nudge.xy, halfSize, radii) - boxDistance(point - nudge.xy, halfSize, radii),
        boxDistance(point + nudge.yx, halfSize, radii) - boxDistance(point - nudge.yx, halfSize, radii)
    );

    return slope / max(length(slope), 0.0001);
}

vec3 gather(vec2 base, vec2 texel, float radius) {
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

    return sum / total;
}

float edge(float dist) {
    float aa = max(fwidth(dist), 1.0e-4);
    return clamp(0.5 - dist / aa, 0.0, 1.0);
}

void main() {
    vec2 halfSize = quadSize * 0.5;
    vec2 point = (texCoord0 - 0.5) * quadSize;
    vec4 radii = vec4(corners.x);

    float dist = boxDistance(point, halfSize, radii);
    float alpha = edge(dist);

    if (alpha <= 0.001) discard;

    vec2 normal = boxNormal(point, halfSize, radii);
    float band = clamp(1.0 + dist / 7.0, 0.0, 1.0);
    float lip = clamp(1.0 + dist / 1.7, 0.0, 1.0);
    float sheen = corners.z / 100.0;

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 bend = vec2(normal.x, -normal.y) * band * band * texel;
    float radius = max(extra, 1.0);

    vec2 base = gl_FragCoord.xy * texel + bend * 8.0;
    vec3 scene = gather(base, texel, radius);

    if (band > 0.1) {
        float red = gather(base + bend * 3.0, texel, radius).r;
        float blue = gather(base - bend * 3.0, texel, radius).b;
        scene = mix(scene, vec3(red, scene.g, blue), band * band);
    }

    vec3 glass = mix(scene, vertexColor.rgb, vertexColor.a);

    float lit = clamp(dot(normal, LIGHT), 0.0, 1.0);
    float back = clamp(-dot(normal, LIGHT), 0.0, 1.0);
    float gloss = band * band * sheen;
    float dust = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);

    glass *= 1.0 - 0.2 * gloss * back;
    glass += gloss * (0.03 + 0.3 * lit + 0.09 * back);
    glass += lip * lip * sheen * (0.09 + 0.15 * lit);
    glass += (dust - 0.5) * 0.014;

    fragColor = vec4(glass, alpha * clamp(corners.w / 100.0, 0.0, 1.0)) * ColorModulator;
}
