#version 330 compatibility
#include "/lib/settings.glsl"
#include "/lib/sky.glsl"
/* const int colortex0Format = RGBA16F; */
/* const int colortex1Format = RGBA16F; */
/* const int colortex2Format = RGBA16F; */
/* const int colortex3Format = RGBA16F; */
uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform float viewWidth, viewHeight, near, far, rainStrength, frameTimeCounter;
uniform int isEyeInWater;
uniform mat4 gbufferProjectionInverse, gbufferModelViewInverse;
uniform vec3 sunPosition;
in vec2 texcoord;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

float lin(float d) { return 2.0 * near * far / (far + near - (2.0 * d - 1.0) * (far - near)); }
float ign(vec2 p) { return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715)))); }

void main() {
    vec3 col = texture(colortex0, texcoord).rgb;
#ifdef AO
    float d = texture(depthtex0, texcoord).r;
    if (d < 1.0) {
        float z = lin(d);
        vec2 px = 1.0 / vec2(viewWidth, viewHeight);
        float rad = clamp(18.0 / (1.0 + z * 0.12), 3.0, 18.0);
        float rot = ign(gl_FragCoord.xy) * 6.2831853;
        float occ = 0.0;
        for (int i = 0; i < 12; i++) {
            float a = rot + float(i) * 2.399963;
            float r = rad * sqrt((float(i) + 0.5) / 12.0);
            vec2 o = vec2(cos(a), sin(a)) * r * px;
            float zn = lin(texture(depthtex0, texcoord + o).r);
            float dz = z - zn;
            float w = clamp(dz / (0.12 * z + 0.25), 0.0, 1.0) * (1.0 - clamp((dz - 2.0) / 3.0, 0.0, 1.0));
            occ += w;
        }
        occ /= 12.0;
        col *= 1.0 - occ * (float(AO_STRENGTH) / 100.0) * 1.1;
    }
#endif
    // mgła atmosferyczna (perspektywa powietrzna) i woda
    {
        float dd = texture(depthtex0, texcoord).r;
        if (dd < 1.0) {
            vec4 vp = gbufferProjectionInverse * vec4(vec3(texcoord, dd) * 2.0 - 1.0, 1.0);
            vec3 view = vp.xyz / vp.w;
            float dist = length(view);
            vec3 dirW = normalize(mat3(gbufferModelViewInverse) * view);
            vec3 sunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
            if (isEyeInWater == 1) {
                float wf = 1.0 - exp(-dist * 0.11);
                col = mix(col, vec3(0.04, 0.22, 0.30) * (0.35 + 0.65 * clamp(sunDir.y + 0.5, 0.0, 1.0)), wf * 0.92);
            } else {
                float dens = mix(0.0016, 0.0075, rainStrength);
                float haze = 1.0 - exp(-dist * dens);
                haze = max(haze, smoothstep(far * 0.62, far * 0.98, dist));
                vec3 hz = skyColor(normalize(vec3(dirW.x, max(dirW.y, 0.0) * 0.4 + 0.015, dirW.z)), sunDir, rainStrength, frameTimeCounter, false);
                col = mix(col, hz, clamp(haze, 0.0, 1.0) * 0.85);
            }
        }
    }
    outColor = vec4(col, 1.0);
}
