package com.weidian.egldemo.oepngl;

import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.weidian.egldemo.oepngl.EGLCore.EGL_RECORDABLE_ANDROID;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class CustomerGLRenderer implements SurfaceHolder.Callback2 {
    private RenderThread mThread = new RenderThread();
    private WeakReference<SurfaceView> mSurfaceView;
    private Surface mSurface;
    private final List<IDrawer> mDrawers = new ArrayList<>();

    public CustomerGLRenderer() {
        mThread.start();
    }

    public void setSurface(SurfaceView surface) {
        mSurfaceView = new WeakReference<>(surface);
        surface.getHolder().addCallback(this);

        surface.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                stop();
            }
        });
    }

    public void setSurface(Surface surface, int width, int height) {
        mSurface = surface;
        mThread.onSurfaceCreate();
        mThread.onSurfaceChange(width, height);
    }

    public void setRenderMode(RenderMode mode) {
        mThread.setRenderMode(mode);
    }

    public void notifySwap(long timeUs) {
        mThread.notifySwap(timeUs);
    }

    public void addDrawer(IDrawer drawer) {
        mDrawers.add(drawer);
    }

    public void stop() {
        mThread.onSurfaceStop();
        mSurface = null;
    }


    @Override
    public void surfaceRedrawNeeded(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mSurface = holder.getSurface();
        mThread.onSurfaceCreate();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mThread.onSurfaceChange(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mThread.onSurfaceDestroy();
    }

    private class RenderThread extends Thread {
        private RenderState mState = RenderState.NO_SURFACE;
        private EGLSurfaceHolder mEGLSurface;
        private boolean mHaveBindEGLContext;
        private boolean mNeverCreateEglContext = true;
        private int mWidth;
        private int mHeight;
        private Object mWaitLock = new Object();
        private Long mCurTimestamp = 0L;
        private long mLastTimestamp;
        private RenderMode mRenderMode = RenderMode.RENDER_WHEN_DIRTY;

        public void holdOn() {
            Log.e("TAG+++", "holdOn");
            synchronized (mWaitLock) {
                try {
                    mWaitLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void  notifyGo() {
            Log.e("TAG+++", "notifyGo");
            synchronized(mWaitLock) {
                mWaitLock.notify();
            }
        }

        public void setRenderMode(RenderMode mode) {
            mRenderMode = mode;
        }

        public void onSurfaceCreate() {
            mState = RenderState.FRESH_SURFACE;
        }

        public void onSurfaceChange(int w, int h) {
            mWidth = w;
            mHeight = h;
            mState = RenderState.SURFACE_CHANGE;
            notifyGo();
        }

        public void onSurfaceDestroy() {
            mState = RenderState.SURFACE_DESTROY;
            notifyGo();
        }

        public void onSurfaceStop() {
            mState = RenderState.STOP;
            notifyGo();
        }

        public void notifySwap(long timeUs) {
            synchronized(mCurTimestamp) {
                mCurTimestamp = timeUs;
            }
            notifyGo();
        }

        @Override
        public void run() {
            super.run();
            initEGL();
            while (true) {
                Log.e("TAG+++", "mState..." + mState);
                if (mState == RenderState.FRESH_SURFACE) {
                    createEGLSurfaceFirst();
                    holdOn();
                } else if (mState == RenderState.SURFACE_CHANGE) {
                    createEGLSurfaceFirst();
                    GLES20.glViewport(0, 0, mWidth, mHeight);
                    configWorldSize();
                    mState = RenderState.RENDERING;
                } else if (mState == RenderState.RENDERING) {
                    render();
                    if (mRenderMode == RenderMode.RENDER_WHEN_DIRTY) {
                        holdOn();
                    }
                } else if (mState == RenderState.SURFACE_DESTROY) {
                    destroyEGLSurface();
                    mState = RenderState.NO_SURFACE;
                } else if (mState == RenderState.STOP) {
                    releaseEGL();
                    return;
                } else {
                    holdOn();
                }
                if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                    try {
                        sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        private void initEGL() {
            mEGLSurface = new EGLSurfaceHolder();
            mEGLSurface.init(null, EGL_RECORDABLE_ANDROID);
        }

        private void createEGLSurfaceFirst() {
            if (!mHaveBindEGLContext) {
                mHaveBindEGLContext = true;
                createEGLSurface();
                if (mNeverCreateEglContext) {
                    mNeverCreateEglContext = false;
                    GLES20.glClearColor(0f, 0f, 0f, 0f);
                    //开启混合，即半透明
                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    generateTextureID();
                }
            }
        }

        private void createEGLSurface() {
            mEGLSurface.createEGLSurface(mSurface, 0, 0);
            mEGLSurface.makeCurrent();
        }

        private void generateTextureID() {
            int textureIds[] = OpenGLTools.createTextureIds(mDrawers.size());
            for (int i = 0; i < mDrawers.size(); i++) {
                IDrawer drawer = mDrawers.get(i);
                drawer.setTextureID(textureIds[i]);
            }
        }

        private void configWorldSize() {
            for (IDrawer drawer : mDrawers) {
                drawer.setWorldSize(mWidth, mHeight);
            }
        }

        private void render() {
            boolean render;
            Log.e("TAG+++", "render...");
            if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                render = true;
            } else {
                synchronized (mCurTimestamp) {
                    if (mCurTimestamp > mLastTimestamp) {
                        mLastTimestamp = mCurTimestamp;
                        render = true;
                    } else {
                        render = false;
                    }
                }
            }

            if (render) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                for (IDrawer drawer : mDrawers) {
                    drawer.draw();
                }
                mEGLSurface.setTimestamp(mCurTimestamp);
                mEGLSurface.swapBuffers();
            }
        }


        private void destroyEGLSurface() {
            mEGLSurface.destroyEGLSurface();
            mHaveBindEGLContext = false;
        }

        private void releaseEGL() {
            mEGLSurface.release();
        }
    }


    enum RenderState {
        NO_SURFACE, //没有有效的surface
        FRESH_SURFACE, //持有一个未初始化的新的surface
        SURFACE_CHANGE, //surface尺寸变化
        RENDERING, //初始化完毕，可以开始渲染
        SURFACE_DESTROY, //surface销毁
        STOP //停止绘制
    }

    enum RenderMode {
        RENDER_CONTINUOUSLY,
        RENDER_WHEN_DIRTY
    }
}
