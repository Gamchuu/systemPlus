#version 330

#moj_import <system:seeds.glsl>

uniform sampler2D SeedSampler;

out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(SeedSampler, 0);

    vec2 best = vec2(0.0);
    float bestDistance = 1.0e20;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            ivec2 tap = pixel + ivec2(x, y) * STEP;
            if (any(lessThan(tap, ivec2(0))) || any(greaterThanEqual(tap, size))) {
                continue;
            }

            vec4 texel = texelFetch(SeedSampler, tap, 0);
            if (!hasSeed(texel)) {
                continue;
            }

            vec2 seed = decodeSeed(texel);
            vec2 delta = seed - vec2(pixel);
            float squared = dot(delta, delta);
            if (squared < bestDistance) {
                bestDistance = squared;
                best = seed;
            }
        }
    }

    fragColor = bestDistance < 1.0e20 ? encodeSeed(best) : NO_SEED;
}
