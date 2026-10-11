#version 330

vec3 fluid(vec2 fragCoord, vec2 resolution, float time, vec3 tint) {
    vec2 w = (2.0 * fragCoord - resolution) / min(resolution.x, resolution.y);

    for (float i = 1.0; i < 10.0; i++) {
        w.x += 0.6 / i * cos(i * 2.5 * w.y + time);
        w.y += 0.6 / i * cos(i * 1.5 * w.x + time);
    }

    float band = abs(sin(time - w.y - w.x));
    return clamp(tint * 0.13 / max(band, 0.05), 0.0, 1.6);
}
