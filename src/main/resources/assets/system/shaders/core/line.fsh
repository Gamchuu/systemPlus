#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 lineCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = abs(lineCoord.x) - lineCoord.y;
    float aa = max(fwidth(lineCoord.x), 1.0e-4);

    vec4 color = vertexColor * ColorModulator;
    color.a *= clamp(0.5 - dist / aa, 0.0, 1.0);
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
