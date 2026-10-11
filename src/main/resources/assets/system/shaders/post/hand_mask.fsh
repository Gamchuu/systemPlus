#version 330

uniform sampler2D DepthSampler;

layout(std140) uniform MaskColor {
    vec4 Color;
};

out vec4 fragColor;

void main() {
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    fragColor = depth < 1.0 ? vec4(Color.rgb, 1.0) : vec4(0.0);
}
