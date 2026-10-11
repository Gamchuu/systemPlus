#version 330

const vec4 NO_SEED = vec4(1.0);

vec4 encodeSeed(vec2 position) {
    return vec4(floor(position.x / 256.0), mod(position.x, 256.0), floor(position.y / 256.0), mod(position.y, 256.0)) / 255.0;
}

vec2 decodeSeed(vec4 texel) {
    vec4 bytes = floor(texel * 255.0 + 0.5);
    return vec2(bytes.r * 256.0 + bytes.g, bytes.b * 256.0 + bytes.a);
}

bool hasSeed(vec4 texel) {
    return any(lessThan(texel, vec4(0.998)));
}
