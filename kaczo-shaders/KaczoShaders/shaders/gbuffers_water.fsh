#version 330 compatibility
#include "/lib/settings.glsl"
#include "/lib/lit.glsl"
uniform sampler2D gtexture;
uniform vec3 cameraPosition;
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 normalW;
in float isWater;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 tex = texture(gtexture, texcoord);
    vec3 feet = feetPos();
    if (isWater < 0.5) {
        vec3 albedo = pow(tex.rgb * glcolor.rgb, vec3(2.2));
        outColor = vec4(shadeLit(albedo, feet, normalize(normalW), lmcoord, 0.0), tex.a * glcolor.a);
        return;
    }
    vec3 wp = feet + cameraPosition;
    float t = frameTimeCounter;
    vec2 q = wp.xz;
    float e = 0.012;
    float h0 = sin(q.x * 1.3 + t * 1.1) * 0.5 + sin(q.y * 1.7 - t * 0.9) * 0.5 + sin((q.x + q.y) * 2.9 + t * 1.7) * 0.25 + sin((q.x - q.y) * 4.1 - t * 2.2) * 0.12;
    float hx = sin((q.x + e) * 1.3 + t * 1.1) * 0.5 + sin(q.y * 1.7 - t * 0.9) * 0.5 + sin((q.x + e + q.y) * 2.9 + t * 1.7) * 0.25 + sin((q.x + e - q.y) * 4.1 - t * 2.2) * 0.12;
    float hz = sin(q.x * 1.3 + t * 1.1) * 0.5 + sin((q.y + e) * 1.7 - t * 0.9) * 0.5 + sin((q.x + q.y + e) * 2.9 + t * 1.7) * 0.25 + sin((q.x - q.y - e) * 4.1 - t * 2.2) * 0.12;
    vec3 n = normalize(vec3(-(hx - h0) / e * 0.045, 1.0, -(hz - h0) / e * 0.045));
    vec3 V = normalize(feet);
    float fres = 0.03 + 0.97 * pow(1.0 - max(dot(-V, n), 0.0), 5.0);
    vec3 sunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    vec3 R = reflect(V, n);
    R.y = abs(R.y);
    vec3 refl = skyColor(R, sunDir, rainStrength, t, true);
    vec3 water = pow(vec3(0.05, 0.28, 0.36) * (0.4 + 0.6 * glcolor.rgb), vec3(2.2));
    vec3 body = shadeLit(water, feet, vec3(0.0, 1.0, 0.0), lmcoord, 0.0);
    vec3 col = mix(body, refl, clamp(fres * 1.15, 0.0, 1.0));
    float sp = pow(max(dot(R, sunDir), 0.0), 600.0) * 6.0 * smoothstep(0.0, 0.15, sunDir.y);
    col += vec3(1.0, 0.9, 0.7) * sp;
    outColor = vec4(col, mix(0.62, 0.97, fres));
}
