#version 330 compatibility
#include "/lib/settings.glsl"
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16F;
const int colortex2Format = RGBA16F;
const int colortex3Format = RGBA16F;
uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform float viewWidth, viewHeight, near, far;
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
    outColor = vec4(col, 1.0);
}
