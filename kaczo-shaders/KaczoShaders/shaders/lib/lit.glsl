// Wspólne oświetlenie: słońce/księżyc z cieniami, niebo jako światło otoczenia, światło pochodni
#include "/lib/sky.glsl"
const int shadowMapResolution = 2048;
const float shadowDistance = 110.0;
uniform sampler2D shadowtex1;
uniform mat4 gbufferModelViewInverse, gbufferProjectionInverse, shadowModelView, shadowProjection;
uniform vec3 shadowLightPosition, sunPosition;
uniform float viewWidth, viewHeight, rainStrength, frameTimeCounter;

vec2 distortShadow(vec2 xy) { float d = length(xy) * 0.86 + 0.14; return xy / d; }

vec3 feetPos() {
    vec3 ndc = vec3(gl_FragCoord.xy / vec2(viewWidth, viewHeight), gl_FragCoord.z) * 2.0 - 1.0;
    vec4 vp = gbufferProjectionInverse * vec4(ndc, 1.0);
    return (gbufferModelViewInverse * vec4(vp.xyz / vp.w, 1.0)).xyz;
}

float shadowFactor(vec3 feet, vec3 nW, float ndl) {
    vec3 f = feet + nW * (0.07 + 0.16 * (1.0 - ndl));
    vec4 sc = shadowProjection * (shadowModelView * vec4(f, 1.0));
    sc.xy = distortShadow(sc.xy);
    sc.z *= 0.2;
    sc.xyz = sc.xyz * 0.5 + 0.5;
    if (sc.x < 0.0 || sc.x > 1.0 || sc.y < 0.0 || sc.y > 1.0 || sc.z > 1.0) return 1.0;
    float rot = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) * 6.2831853;
    float texel = 1.0 / float(shadowMapResolution);
    float bias = 0.00035 + 0.0006 * (1.0 - ndl);
    float lit = 0.0;
    for (int i = 0; i < 9; i++) {
        float a = rot + float(i) * 2.399963;
        float r = sqrt((float(i) + 0.5) / 9.0) * 2.2 * texel;
        float d = texture(shadowtex1, sc.xy + vec2(cos(a), sin(a)) * r).r;
        lit += step(sc.z - bias, d);
    }
    return lit / 9.0;
}

// albedo – w liniowej przestrzeni, zwraca kolor w gamma
vec3 shadeLit(vec3 albedo, vec3 feet, vec3 nW, vec2 lm, float emissive) {
    vec3 lightDir = normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);
    vec3 sunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    float ndl = max(dot(nW, lightDir), 0.0);
    float sunUp = smoothstep(-0.04, 0.14, lightDir.y);
    float skyL = smoothstep(0.0, 0.95, lm.y);
    float sh = shadowFactor(feet, nW, ndl);
    float distFade = smoothstep(shadowDistance * 0.8, shadowDistance, length(feet.xz));
    sh = mix(sh, 1.0, distFade);
    sh = mix(0.0, sh, smoothstep(0.25, 0.8, lm.y));

    bool isDay = sunDir.y > -0.08;
    vec3 sunTint = mix(vec3(1.0, 0.50, 0.22), vec3(1.0, 0.93, 0.80), smoothstep(0.0, 0.45, lightDir.y));
    vec3 direct = isDay ? sunTint * 3.0 : vec3(0.30, 0.42, 0.85) * 0.55;
    direct *= (1.0 - rainStrength * 0.75) * sunUp;

    vec3 zen = pow(skyColor(vec3(0.0, 1.0, 0.0), sunDir, rainStrength, frameTimeCounter, false), vec3(2.2));
    vec3 hor = pow(skyColor(normalize(vec3(nW.x, 0.15, nW.z) + 1e-4), sunDir, rainStrength, frameTimeCounter, false), vec3(2.2));
    float up = nW.y * 0.5 + 0.5;
    vec3 ambient = mix(hor * 0.55 + vec3(0.02, 0.018, 0.015), zen * 0.9, up) * (0.35 + 0.65 * skyL);
    ambient = mix(vec3(dot(ambient, vec3(0.333))), ambient, 0.7) * 2.1;
    ambient = max(ambient, vec3(0.02, 0.022, 0.03));

    float bl = pow(clamp(lm.x, 0.0, 1.0), 3.2);
    vec3 block = vec3(1.0, 0.55, 0.24) * bl * 2.2 * (1.0 - 0.6 * skyL * sunUp);

    vec3 lit = albedo * (ambient + direct * ndl * sh * skyL + block) + albedo * emissive;
    lit = 1.0 - exp(-lit * 1.35);
    return pow(lit, vec3(1.0 / 2.2));
}
