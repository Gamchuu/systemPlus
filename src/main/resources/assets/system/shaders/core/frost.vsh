#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 local;
out vec4 vertexColor;
out vec2 size;
out float radius;
out float opacity;
out float spread;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    local = UV0;
    vertexColor = Color;
    size = vec2(UV1) / 16.0;
    radius = float(UV2.x) / 16.0;
    opacity = float(UV2.y) / 1000.0;
    spread = Normal.z * 127.0;
}
