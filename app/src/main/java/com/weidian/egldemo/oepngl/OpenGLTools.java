package com.weidian.egldemo.oepngl;

import android.opengl.GLES20;


import java.nio.Buffer;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class OpenGLTools {
    public static final int[] createTextureIds(int count) {
        int[] texture = new int[count];
        GLES20.glGenTextures(count, texture, 0);
        return texture;
    }

    public static final int createFBOTexture(int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(3553, textures[0]);
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, (Buffer)null);
        GLES20.glTexParameterf(3553, 10241, (float)9728);
        GLES20.glTexParameterf(3553, 10240, (float)9729);
        GLES20.glTexParameterf(3553, 10242, (float)'脯');
        GLES20.glTexParameterf(3553, 10243, (float)'脯');
        GLES20.glBindTexture(3553, 0);
        return textures[0];
    }

    public static final int createFrameBuffer() {
        int[] fbs = new int[1];
        GLES20.glGenFramebuffers(1, fbs, 0);
        return fbs[0];
    }

    public static final void bindFBO(int fb, int textureId) {
        GLES20.glBindFramebuffer(36160, fb);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
    }

    public static final void unbindFBO() {
        GLES20.glBindRenderbuffer(36161, 0);
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glBindTexture(3553, 0);
    }

    public static final void deleteFBO(int[] frame,  int[] texture) {
        GLES20.glBindRenderbuffer(36161, 0);
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glDeleteFramebuffers(1, frame, 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glDeleteTextures(1, texture, 0);
    }
}
