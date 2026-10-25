#ifndef KACZO_SKY
#define KACZO_SKY
float hash21(vec2 p) { p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1, 0)), f.x), mix(hash21(i + vec2(0, 1)), hash21(i + vec2(1, 1)), f.x), f.y);
}
float fbm(vec2 p) {
    float a = 0.5, s = 0.0;
    for (int i = 0; i < 4; i++) { s += a * vnoise(p); p = p * 2.03 + 17.1; a *= 0.5; }
    return s;
}

// dir – kierunek patrzenia w świecie, sunDir – kierunek do słońca (świat), rain 0..1, t – czas
vec3 skyColor(vec3 dir, vec3 sunDir, float rain, float t, bool withClouds) {
    float sy = sunDir.y;
    float day = smoothstep(-0.12, 0.30, sy);
    float twi = exp(-pow(sy / 0.20, 2.0));
    float h = clamp(dir.y, -1.0, 1.0);
    vec3 zen = mix(vec3(0.004, 0.008, 0.030), vec3(0.09, 0.28, 0.70), day);
    vec3 hor = mix(vec3(0.020, 0.036, 0.080), vec3(0.60, 0.76, 0.96), day);
    vec2 hd = normalize(dir.xz + 1e-4), sd2 = normalize(sunDir.xz + 1e-4);
    float toSun = pow(max(dot(hd, sd2), 0.0), 3.0);
    hor = mix(hor, vec3(1.0, 0.46, 0.20), twi * (0.30 + 0.65 * toSun));
    zen = mix(zen, vec3(0.16, 0.16, 0.40), twi * 0.35);
    float g = pow(1.0 - max(h, 0.0), 3.4);
    vec3 col = mix(zen, hor, g);
    col = mix(col, hor * 0.45, smoothstep(0.0, -0.3, h));

    float sd = max(dot(dir, sunDir), 0.0);
    vec3 sunCol = mix(vec3(1.0, 0.45, 0.18), vec3(1.0, 0.92, 0.75), smoothstep(0.0, 0.45, sy));
    col += sunCol * (pow(sd, 5.0) * 0.20 + pow(sd, 48.0) * 0.55 + pow(sd, 1200.0) * 2.5) * smoothstep(-0.18, 0.06, sy);
    float md = max(dot(dir, -sunDir), 0.0);
    col += vec3(0.32, 0.44, 0.80) * (pow(md, 10.0) * 0.045 + pow(md, 400.0) * 0.5) * (1.0 - day);

    if (withClouds && dir.y > 0.01) {
        vec2 p = dir.xz / (dir.y + 0.14) * 1.1 + vec2(t * 0.006, t * 0.003);
        float n = fbm(p * 1.7);
        float c = smoothstep(0.50, 0.80, n) * smoothstep(0.01, 0.28, dir.y);
        vec3 cc = mix(vec3(0.07, 0.08, 0.13), vec3(1.0), day);
        cc = mix(cc, vec3(1.0, 0.62, 0.42), twi * (0.4 + 0.5 * toSun));
        col = mix(col, cc, c * 0.55 * (1.0 - rain));
    }
    float gray = dot(col, vec3(0.333));
    return mix(col, vec3(gray) * 0.72, rain * 0.8);
}
#endif
