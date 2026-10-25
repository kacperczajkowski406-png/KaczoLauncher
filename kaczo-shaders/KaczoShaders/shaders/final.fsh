#version 330 compatibility
#include "/lib/settings.glsl"
uniform sampler2D colortex0;
uniform sampler2D colortex2;
uniform sampler2D colortex3;
uniform sampler2D depthtex0;
uniform float viewWidth, viewHeight, frameTimeCounter, sunAngle, rainStrength;
uniform int isEyeInWater;
uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform mat4 gbufferProjection;
in vec2 texcoord;
layout(location = 0) out vec4 outColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233)) + frameTimeCounter) * 43758.5453); }
float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

void main() {
    vec2 uv = texcoord;
    vec2 dir = uv - 0.5;
    vec3 scene;
#ifdef CHROMA
    vec2 off = dir * dot(dir, dir) * 0.016;
    scene = vec3(texture(colortex0, uv + off).r, texture(colortex0, uv).g, texture(colortex0, uv - off).b);
#else
    scene = texture(colortex0, uv).rgb;
#endif

    float bloomK = float(BLOOM) / 100.0;
    float streakK = 0.0;
#if STYLE == 1
    bloomK *= 1.25; streakK = 0.9;
#elif STYLE == 2
    bloomK *= 1.6; streakK = 0.35;
#else
    streakK = 0.5;
#endif
#ifndef STREAK
    streakK = 0.0;
#endif
    vec3 col = scene + texture(colortex2, uv).rgb * bloomK * 0.9 + texture(colortex3, uv).rgb * streakK * bloomK * 0.9;

#ifdef GODRAYS
    {
        bool day = sunAngle < 0.5;
        vec3 lp = day ? sunPosition : moonPosition;
        if (lp.z < 0.0 && rainStrength < 0.9) {
            vec4 p = gbufferProjection * vec4(lp, 1.0);
            vec2 sc = p.xy / p.w * 0.5 + 0.5;
            vec2 delta = (sc - uv) / 28.0;
            vec2 s = uv;
            float illum = 1.0, acc = 0.0;
            vec3 ray = vec3(0.0);
            float jit = hash(gl_FragCoord.xy);
            s += delta * jit;
            for (int i = 0; i < 28; i++) {
                s += delta;
                if (s.x < 0.0 || s.x > 1.0 || s.y < 0.0 || s.y > 1.0) break;
                float sky = step(0.9999, texture(depthtex0, s).r);
                ray += texture(colortex0, s).rgb * sky * illum;
                illum *= 0.955;
            }
            ray /= 28.0;
            float facing = clamp(1.0 - length(sc - 0.5) * 0.9, 0.0, 1.0);
            col += ray * (day ? 0.55 : 0.2) * facing * (1.0 - rainStrength);
        }
    }
#endif

    // miękkie „ramię” jasności: słońce i bloom nie wypalają kolorów
    {
        vec3 over = max(col - 0.82, 0.0);
        col = min(col, vec3(0.82)) + 0.18 * (1.0 - exp(-over * 4.0));
    }
    // grading
    float l = luma(col);
#if STYLE == 1
    // Noir Glass: czarno-białe szkło, mocny kontrast, zimny odcień
    vec3 g = vec3(l);
    col = mix(g, col, 0.10);
    col = pow(max(col, 0.0), vec3(1.18));
    col *= vec3(0.97, 0.99, 1.03) * 1.12;
#elif STYLE == 2
    // Dream: miękkie, pastelowe, podniesione cienie
    col = mix(vec3(l), col, 1.22);
    col = col * 0.92 + 0.045;
    col = mix(col, col * vec3(1.05, 0.97, 1.08), 0.6);
#else
    // Cinematic: ciepłe światła, chłodne cienie, lekki kontrast
    vec3 shadowTint = vec3(0.90, 0.98, 1.08), highTint = vec3(1.07, 1.02, 0.93);
    col *= mix(shadowTint, highTint, smoothstep(0.1, 0.8, l));
    col = mix(vec3(luma(col)), col, 1.12);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.35);
#endif
    {
        float sat = float(SAT) / 100.0;
        col = mix(vec3(luma(col)), col, sat);
    }
    if (isEyeInWater == 1) col *= vec3(0.78, 0.95, 1.12);

#if VIGNETTE > 0
    float v = clamp(length(dir * vec2(1.0, viewHeight / viewWidth * 1.15)) * 1.55, 0.0, 1.5);
    col *= 1.0 - float(VIGNETTE) / 100.0 * pow(v, 2.4) * 0.85;
#endif
#ifdef GRAIN
    col += (hash(gl_FragCoord.xy) - 0.5) * 0.022;
#endif
    outColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
