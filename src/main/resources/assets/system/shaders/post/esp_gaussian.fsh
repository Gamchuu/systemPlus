#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

float weight(float x) {
    float sigma = float(RADIUS) / 2.5;
    return exp(-(x * x) / (2.0 * sigma * sigma));
}



void main() {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
#ifdef HORIZONTAL
    vec2 direction = vec2(texel.x, 0.0);
#else
    vec2 direction = vec2(0.0, texel.y);
#endif

    vec3 color = texture(InSampler, texCoord).rgb;
    float total = 1.0;

    for (int i = 1; i <= RADIUS; i += 2) {
        float w1 = weight(float(i));
        float w2 = weight(float(i + 1));
        float w = w1 + w2;
        vec2 offset = direction * ((float(i) * w1 + float(i + 1) * w2) / w);

        color += (texture(InSampler, texCoord + offset).rgb + texture(InSampler, texCoord - offset).rgb) * w;
        total += 2.0 * w;
    }

    fragColor = vec4(color / total, 1.0);
}
