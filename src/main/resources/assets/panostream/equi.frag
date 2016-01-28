#version 110

#define PI 3.141592653589793238462643383279

#define IMAGE_FRONT 0
#define IMAGE_BACK 1
#define IMAGE_LEFT 2
#define IMAGE_RIGHT 3
#define IMAGE_BOTTOM 4
#define IMAGE_TOP 5

//the six input textures
uniform sampler2D frontTex;
uniform sampler2D backTex;
uniform sampler2D leftTex;
uniform sampler2D rightTex;
uniform sampler2D bottomTex;
uniform sampler2D topTex;

void main() {
    float x = gl_TexCoord[0].x;
    float y = gl_TexCoord[0].y;

    float yaw = PI * 2.0 * x;
    int piQuarter = int(8.0 * x) - 4;

    //determine which texture has to be mapped to the current pixel
    int target = IMAGE_BACK;
    if(piQuarter < -3) {
        target = IMAGE_BACK;
    } else if(piQuarter < -1) {
        target = IMAGE_LEFT;
    } else if(piQuarter < 1) {
        target = IMAGE_FRONT;
    } else if(piQuarter < 3) {
        target = IMAGE_RIGHT;
    }
    float fYaw = mod(yaw + PI/4.0, (PI/2.0)) - PI/4.0;
    float d = 1.0 / cos(fYaw);
    float gcXN = (tan(fYaw) + 1.0) / 2.0;

    float pitch = PI * y - PI / 2.0;

    float cXN = gcXN;
    float cYN = (tan(pitch) * d + 1.0) / 2.0;

    if(cYN >= 1.0) {
        float pd = tan(PI/2.0 - pitch);
        cXN = (-1.0 * sin(yaw) * pd + 1.0) / 2.0;
        cYN = (cos(yaw) * pd + 1.0) / 2.0;
        target = IMAGE_TOP;
    }
    if(cYN < 0.0) {
        float pd = tan(PI/2.0 - pitch);
        cXN = (sin(yaw) * pd + 1.0) / 2.0;
        cYN = (cos(yaw) * pd + 1.0) / 2.0;
        target = IMAGE_BOTTOM;
    }

    if(target == IMAGE_FRONT) {
        gl_FragColor = texture2D(frontTex, vec2(cXN, cYN));
    } else if(target == IMAGE_BACK) {
        gl_FragColor = texture2D(backTex, vec2(cXN, cYN));
    } else if(target == IMAGE_LEFT) {
        gl_FragColor = texture2D(leftTex, vec2(cXN, cYN));
    } else if(target == IMAGE_RIGHT) {
        gl_FragColor = texture2D(rightTex, vec2(cXN, cYN));
    } else if(target == IMAGE_BOTTOM) {
        gl_FragColor = texture2D(bottomTex, vec2(cXN, cYN));
    } else if(target == IMAGE_TOP) {
        gl_FragColor = texture2D(topTex, vec2(cXN, cYN));
    }
}