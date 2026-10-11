#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec3 ray;
in vec4 vertexColor;
in vec2 clock;
in vec4 dials;
in float extra;

out vec4 fragColor;

const vec3 DAY_HIGH = vec3(0.13, 0.32, 0.70);
const vec3 DAY_LOW = vec3(0.60, 0.77, 0.94);
const vec3 NIGHT_HIGH = vec3(0.010, 0.016, 0.062);
const vec3 NIGHT_LOW = vec3(0.055, 0.075, 0.170);

float speck(vec2 point) {
    return fract(sin(dot(point, vec2(41.317, 289.71))) * 21759.13);
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

float layered(vec2 point) {
    float sum = 0.0;
    float weight = 0.5;

    for (int i = 0; i < 5; i++) {
        sum += smoke(point) * weight;
        point *= 2.03;
        weight *= 0.5;
    }

    return sum;
}

void main() {
    vec3 dir = normalize(ray);

    float tint = dials.x / 100.0;
    float night = dials.y / 100.0;
    float ribbons = dials.z / 100.0;
    float stars = dials.w / 100.0;
    float clouds = extra / 100.0;
    float time = clock.x;

    float lift = clamp(dir.y, -1.0, 1.0);
    float rise = pow(clamp(lift * 0.5 + 0.5, 0.0, 1.0), 0.85);

    vec3 high = mix(DAY_HIGH, NIGHT_HIGH, night);
    vec3 low = mix(DAY_LOW, NIGHT_LOW, night);
    vec3 sky = mix(low, high, rise);

    sky = mix(sky, vertexColor.rgb * (0.22 + 0.78 * rise), tint * (0.28 + 0.55 * rise));

    float seam = exp(-abs(lift) * 8.5);
    sky += vertexColor.rgb * seam * tint * (0.20 + 0.34 * night);

    float turn = (clock.y - 0.25) * 6.2831853;
    vec3 toSun = normalize(vec3(sin(turn), cos(turn), 0.0));

    float sunward = dot(dir, toSun);
    float moonward = -sunward;

    vec3 sunHue = mix(vec3(1.0, 0.95, 0.84), vertexColor.rgb, tint * 0.45);
    float disc = smoothstep(0.9986, 0.9994, sunward);
    float flare = pow(clamp(sunward, 0.0, 1.0), 24.0);
    float wash = pow(clamp(sunward, 0.0, 1.0), 4.0);

    sky += sunHue * (disc * 2.6 + flare * 1.05 + wash * 0.16) * (1.0 - night * 0.8);

    float moonFace = smoothstep(0.9990, 0.9996, moonward);
    float moonRing = pow(clamp(moonward, 0.0, 1.0), 42.0);

    sky += vec3(0.84, 0.88, 1.0) * (moonFace * 2.0 + moonRing * 0.45) * night;

    vec2 sheet = dir.xz / (abs(lift) + 0.30);

    if (clouds > 0.001 && lift > 0.0) {
        vec2 deck = sheet * 0.55 + vec2(time * 0.013, time * 0.005);

        float body = layered(deck);
        body = smoothstep(0.54 - clouds * 0.24, 0.80, body);

        float side = clamp(sunward * 0.5 + 0.5, 0.0, 1.0);
        vec3 hue = mix(vec3(0.40, 0.45, 0.54), vec3(1.0, 0.97, 0.92), side);
        hue = mix(hue, hue * 0.30 + vertexColor.rgb * 0.34, night * 0.85);

        sky = mix(sky, hue, body * smoothstep(0.0, 0.20, lift) * clouds * 0.85);
    }

    if (ribbons > 0.001) {
        float veil = layered(sheet * 0.62 + vec2(time * 0.055, time * 0.018));
        veil = pow(clamp(veil, 0.0, 1.0), 2.1);

        float band = sin(lift * 8.0 + veil * 6.0 + time * 0.22) * 0.5 + 0.5;
        band *= smoothstep(0.015, 0.52, lift);

        sky += vertexColor.rgb * band * veil * ribbons * (0.40 + 0.90 * night);
    }

    if (stars > 0.001 && night > 0.01) {
        float dust = layered(sheet * 0.9 + 17.0);
        float lane = exp(-pow((lift - 0.28) * 2.6, 2.0) * 3.0);

        sky += mix(vertexColor.rgb, vec3(0.62, 0.70, 1.0), 0.5) * dust * dust * lane * night * stars * 0.30;

        vec3 grid = dir * 52.0;
        vec3 cell = floor(grid);
        float pick = fract(sin(dot(cell, vec3(12.9898, 78.233, 37.719))) * 43758.545);

        if (pick > 0.971) {
            vec3 jog = vec3(fract(pick * 13.7), fract(pick * 29.3), fract(pick * 47.9)) - 0.5;
            float near = length(fract(grid) - 0.5 - jog * 0.5);

            float size = fract(pick * 71.17);
            float twist = fract(pick * 197.31);

            float core = smoothstep(0.09 + size * 0.05, 0.0, near);
            float bloom = smoothstep(0.32 + size * 0.15, 0.0, near);

            float beat = 0.60 + 0.40 * sin(time * (0.5 + twist * 2.6) + pick * 61.0);
            float lamp = (core * 1.7 + bloom * 0.30) * beat * (0.40 + size * 0.85);

            sky += mix(vec3(0.82, 0.88, 1.0), vec3(1.0, 0.87, 0.74), twist)
                    * lamp * stars * night * smoothstep(-0.05, 0.18, lift);
        }

        float slot = floor(time * 0.22);
        float seed = fract(sin(slot * 91.7) * 4177.13);

        if (seed > 0.62) {
            float along = fract(time * 0.22);
            vec2 from = vec2(seed * 2.4 - 1.2, 0.95 + fract(seed * 17.3) * 0.5);
            vec2 head = from + vec2(0.7, -0.55) * along;
            vec2 here = vec2(sheet.x * 0.3, lift * 1.6);

            vec2 gap = here - head;
            float trail = smoothstep(0.30, 0.0, length(gap + vec2(0.7, -0.55) * 0.16));
            float spot = smoothstep(0.055, 0.0, length(gap));

            float burn = smoothstep(0.0, 0.12, along) * smoothstep(1.0, 0.35, along);

            sky += vec3(1.0, 0.94, 0.86) * (spot * 1.8 + trail * 0.35) * burn * stars * night;
        }
    }

    fragColor = vec4(sky, 1.0) * ColorModulator;
}
