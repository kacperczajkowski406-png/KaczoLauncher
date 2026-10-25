#version 330 compatibility
#include "/lib/settings.glsl"
#include "/lib/lit.glsl"
uniform sampler2D gtexture;
uniform vec4 entityColor;
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 normalW;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.1) discard;
    vec3 albedo = pow(tex.rgb * glcolor.rgb, vec3(2.2));
    albedo = mix(albedo, entityColor.rgb, entityColor.a * 0.6);
    float emissive = lmcoord.x > 0.97 ? 0.35 : 0.0;
    vec3 col = shadeLit(albedo, feetPos(), normalize(normalW), lmcoord, emissive);
    outColor = vec4(col, tex.a * glcolor.a);
}
