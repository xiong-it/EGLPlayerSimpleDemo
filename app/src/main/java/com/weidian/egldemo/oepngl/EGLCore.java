package com.weidian.egldemo.oepngl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

/**
 * @description: egl操作封装
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class EGLCore {
    public static final int FLAG_RECORDABLE = 0x01;
    // Android 指定的标志
    // 告诉EGL它创建的surface必须和视频编解码器兼容。
    // 没有这个标志，EGL可能会使用一个MediaCodec不能理解的Buffer
    // 这个变量在api26以后系统才自带有，为了兼容，我们自己写好这个值0x3142
    public static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEglConfig;

    public EGLCore() {
    }

    public void init(EGLContext context, int flag) {
        EGLContext shareContext = context == null ? EGL14.EGL_NO_CONTEXT : context;
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("没有显示设备");
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, versions, 0, versions, 1)) {
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
            throw new RuntimeException("没有显示设备2");
        }
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            EGLConfig config = getConfig(flag, 2);
            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            int attr2List[] = new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            EGLContext eglContext = EGL14.eglCreateContext(mEglDisplay, config, shareContext, attr2List, 0);
            mEglContext = eglContext;
            mEglConfig = config;
        }
    }

    private EGLConfig getConfig(int flag, int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            // 配置EGL 3
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        int attrList[] = new int[]{
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0, // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        };
        //配置Android指定的标记
        if ((flag & FLAG_RECORDABLE) != 0) {
            attrList[attrList.length - 3] = EGL_RECORDABLE_ANDROID;
            attrList[attrList.length - 2] = 1;
        }
        EGLConfig configs[] = new EGLConfig[1];
        int numConfigs[] = new int[1];

        //获取EGL配置
        if (!EGL14.eglChooseConfig(mEglDisplay, attrList, 0,
                configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w("TAG+++", "Unable to find RGB8888 / $version EGLConfig");
            return null;
        }
        //使用系统推荐的第一个配置
        return configs[0];
    }

    /**
     * 创建可显示的渲染缓存
     *
     * @param surface 渲染窗口的surface
     */
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("Invalid surface: " + surface);
        }
        int surfaceAttr[] = new int[]{EGL14.EGL_NONE};
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surfaceAttr, 0);
        if (eglSurface == null) {
            throw new RuntimeException("Surface was null");
        }
        return eglSurface;
    }

    /**
     * 创建离屏渲染缓存
     *
     * @param width  缓存窗口宽
     * @param height 缓存窗口高
     */
    public EGLSurface createOffScreenSurface(int width, int height) {
        int surfaceAttr[] = new int[]{EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE};
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttr, 0);
        if (eglSurface == null) {
            throw new RuntimeException("Surface was null");
        }
        return eglSurface;
    }

    /**
     * 将当前线程与上下文进行绑定
     */
    public void makeCurrent(EGLSurface surface) {
        if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mEglContext)) {
            throw new RuntimeException("makeCurrent(eglSurface) failed");
        }
    }

    /**
     * 将缓存图像数据发送到设备进行显示
     */
    public boolean swapBuffers(EGLSurface surface) {
        return EGL14.eglSwapBuffers(mEglDisplay, surface);
    }

    /**
     * 设置当前帧的时间，单位：纳秒
     */
    public void setPt(EGLSurface surface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, nsecs);
    }

    /**
     * 销毁EGLSurface，并解除上下文绑定
     */
    public void destroySurface(EGLSurface surface) {
        EGL14.eglMakeCurrent(
                mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
        );
        EGL14.eglDestroySurface(mEglDisplay, surface);
    }

    public void release() {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(
                    mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
            );
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEglDisplay);
        }

        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglConfig = null;
    }

}
