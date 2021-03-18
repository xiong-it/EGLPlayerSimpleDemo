package com.weidian.egldemo.oepngl;

import android.opengl.EGLContext;
import android.opengl.EGLSurface;

import androidx.annotation.Nullable;


/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class EGLSurfaceHolder {
    private final String TAG = "EGLSurfaceHolder";
    private EGLCore mEGLCore;
    private EGLSurface mEGLSurface;

    public final void init(@Nullable EGLContext shareContext, int flags) {
        this.mEGLCore = new EGLCore();
        EGLCore var10000 = this.mEGLCore;
        var10000.init(shareContext, flags);
    }

    // $FF: synthetic method
    public static void init$default(EGLSurfaceHolder var0, EGLContext var1, int var2, int var3, Object var4) {
        if ((var3 & 1) != 0) {
            var1 = (EGLContext) null;
        }

        var0.init(var1, var2);
    }

    public final void createEGLSurface(@Nullable Object surface, int width, int height) {
        EGLCore var10001;
        EGLSurface var4;
        if (surface != null) {
            var10001 = this.mEGLCore;

            var4 = var10001.createWindowSurface(surface);
        } else {
            var10001 = this.mEGLCore;

            var4 = var10001.createOffScreenSurface(width, height);
        }

        this.mEGLSurface = var4;
    }

    // $FF: synthetic method
    public static void createEGLSurface$default(EGLSurfaceHolder var0, Object var1, int var2, int var3, int var4, Object var5) {
        if ((var4 & 2) != 0) {
            var2 = -1;
        }

        if ((var4 & 4) != 0) {
            var3 = -1;
        }

        var0.createEGLSurface(var1, var2, var3);
    }

    public final void makeCurrent() {
        if (this.mEGLSurface != null) {
            EGLCore var10000 = this.mEGLCore;

            EGLSurface var10001 = this.mEGLSurface;

            var10000.makeCurrent(var10001);
        }

    }

    public final void swapBuffers() {
        if (this.mEGLSurface != null) {
            EGLCore var10000 = this.mEGLCore;

            EGLSurface var10001 = this.mEGLSurface;

            var10000.swapBuffers(var10001);
        }

    }

    public final void setTimestamp(long timeMs) {
        if (this.mEGLSurface != null) {
            mEGLCore.setPt(mEGLSurface, timeMs * (long) 1000);
        }

    }

    public final void destroyEGLSurface() {
        if (this.mEGLSurface != null) {
            EGLCore var10000 = this.mEGLCore;
            if (var10000 == null) {
            }

            EGLSurface var10001 = this.mEGLSurface;
            if (var10001 == null) {
            }

            var10000.destroySurface(var10001);
            this.mEGLSurface = (EGLSurface) null;
        }

    }

    public final void release() {
        EGLCore var10000 = this.mEGLCore;
        if (var10000 == null) {
        }

        var10000.release();
    }
}
