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

const int HALO = 16;
const float GOLDEN = 2.39996323;
const vec3 EMBER = vec3(1.0, 0.68, 0.32);

float speck(vec2 point) {
    return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453);
}

float smoke(vec2 point) {
    vec2 cell = floor(point);
    vec2 part = fract(point);

    part = part * part * (3.0 - 2.0 * part);

    float a = speck(cell);
    float b = speck(cell + vec2(1.0, 0.0));
    float c = speck(cell + vec2(0.0, 1.0));
    float d = speck(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, part.x), mix(c, d, part.x), part.y);
}

float halo(vec2 base, vec2 texel, float spread, vec2 low, vec2 high) {
    float sum = 0.0;

    for (int i = 0; i < HALO; i++) {
        float step = float(i) + 0.5;
        float angle = step * GOLDEN;
        float reach = sqrt(step / float(HALO)) * spread;

        vec2 at = base + vec2(cos(angle), sin(angle)) * reach * texel;
        sum += texture(Sampler0, clamp(at, low, high)).a;
    }

    return sum / float(HALO);
}

void main() {
    float gone = clamp(corners.x / 100.0, 0.0, 1.0);
    float born = clamp(corners.y / 100.0, 0.0, 1.0);

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));

    vec2 low = (quadSize + 0.5) * texel;
    vec2 high = (quadSize + corners.zw - 0.5) * texel;

    vec2 drift = vec2(0.0, gone * gone * 7.0) * texel;
    vec2 split = vec2(gone * 3.0 + (1.0 - born) * 2.0, 0.0) * texel;
    vec2 base = texCoord0 - drift;

    float red = texture(Sampler0, clamp(base + split, low, high)).a;
    float green = texture(Sampler0, clamp(base, low, high)).a;
    float blue = texture(Sampler0, clamp(base - split, low, high)).a;

    float body = max(green, max(red, blue));

    float grain = smoke(texCoord0 * vec2(74.0, 48.0)) * 0.72 + (1.0 - texCoord0.y) * 0.28;
    float burn = smoothstep(gone - 0.2, gone + 0.03, grain);
    float lip = clamp((0.3 - abs(grain - gone)) / 0.3, 0.0, 1.0) * step(0.015, gone);

    float rise = smoothstep(0.0, 0.85, born);
    float glow = halo(base, texel, extra + gone * 7.0, low, high) * (0.55 + 0.6 * gone);

    float alpha = max(body * burn, glow * glow * burn * 0.6) * rise * vertexColor.a;

    if (alpha <= 0.003) discard;

    vec3 chroma = vec3(red, green, blue) / max(body, 0.0001);
    vec3 colour = vertexColor.rgb * mix(vec3(1.0), chroma, min(1.0, gone + (1.0 - born)));

    colour += lip * lip * EMBER * 1.7;
    colour += glow * vertexColor.rgb * 0.8;
    colour += (1.0 - rise) * EMBER * 0.3;

    fragColor = vec4(colour, alpha) * ColorModulator;
}
