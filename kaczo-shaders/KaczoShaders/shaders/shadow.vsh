#version 330 compatibility
out vec2 texcoord;
out vec4 glcolor;
vec2 distortShadow(vec2 xy) { float d = length(xy) * 0.86 + 0.14; return xy / d; }
void main() {
    gl_Position = ftransform();
    gl_Position.xy = distortShadow(gl_Position.xy);
    gl_Position.z *= 0.2;
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
}
