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

const int TAPS = 96;
const float GOLDEN = 2.39996323;

void main() {
    float spread = max(extra, 1.0);
    vec2 atlas = vec2(textureSize(Sampler0, 0));
    vec2 glyph = quadSize - vec2(spread * 2.0);

    if (glyph.x <= 0.0 || glyph.y <= 0.0) discard;

    vec2 origin = vec2(corners.x, corners.y);
    vec2 local = texCoord0 * quadSize - vec2(spread);
    float shown = clamp(corners.z / 100.0, 0.0, 1.0);

    float sum = 0.0;
    float total = 0.0;

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831;

    for (int i = 0; i < TAPS; i++) {
        float index = float(i) + 0.5;
        float reach = sqrt(index / float(TAPS));
        float angle = index * GOLDEN + dither;

        vec2 offset = vec2(cos(angle), sin(angle)) * reach * spread;
        vec2 point = local + offset;
        float weight = exp(-reach * reach * 2.2);

        total += weight;

        if (point.x < 0.0 || point.y < 0.0 || point.x > glyph.x || point.y > glyph.y)
            continue;

        sum += texture(Sampler0, (origin + point) / atlas).a * weight;
    }

    float glow = sum / max(total, 0.0001);
    glow = pow(clamp(glow * 1.8, 0.0, 1.0), 0.8);

    if (glow <= 0.004) discard;

    vec3 tint = vertexColor.rgb * (1.0 + glow * 0.5);

    fragColor = vec4(tint, glow * shown * vertexColor.a) * ColorModulator;
}
