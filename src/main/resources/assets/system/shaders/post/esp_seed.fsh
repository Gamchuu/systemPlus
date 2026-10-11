#version 330

#moj_import <system:seeds.glsl>

uniform sampler2D SilhouetteSampler;

out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    fragColor = texelFetch(SilhouetteSampler, pixel, 0).a > 0.0 ? encodeSeed(vec2(pixel)) : NO_SEED;
}
