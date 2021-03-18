package com.weidian.egldemo.media;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.weidian.egldemo.IDecoderStateListener;


import java.nio.ByteBuffer;

/**
 * @description: 视频解码
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public final class VideoDecoder extends BaseDecoder {
    private final String TAG;
    private final SurfaceView mSurfaceView;
    private Surface mSurface;

    public VideoDecoder(@NonNull String path, @Nullable SurfaceView sfv, @Nullable Surface surface) {
        super(path);
        this.TAG = "VideoDecoder";
        this.mSurfaceView = sfv;
        this.mSurface = surface;
    }

    public boolean check() {
        if (this.mSurfaceView == null && this.mSurface == null) {
            Log.w(this.TAG, "SurfaceView和Surface都为空，至少需要一个不为空");
            IDecoderStateListener listener = this.getMStateListener();
            if (listener != null) {
                listener.decoderError((BaseDecoder) this, "显示器为空");
            }

            return false;
        } else {
            return true;
        }
    }

    @NonNull
    public IExtractor initExtractor(@NonNull String path) {
        return (IExtractor) (new VideoExtractor(path));
    }

    public void initSpecParams(@NonNull MediaFormat format) {

    }

    public boolean configCodec(@NonNull final MediaCodec codec, @NonNull final MediaFormat format) {
        if (this.mSurface != null) {
            codec.configure(format, this.mSurface, (MediaCrypto) null, 0);
            this.notifyDecode();
        } else if (mSurfaceView != null && mSurfaceView.getHolder() != null && mSurfaceView.getHolder().getSurface() != null){
            mSurface = mSurfaceView.getHolder().getSurface();
            configCodec(codec, format);
        } else {
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback2() {
                public void surfaceRedrawNeeded(SurfaceHolder holder) {
                }

                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                public void surfaceDestroyed(SurfaceHolder holder) {
                }

                public void surfaceCreated(SurfaceHolder holder) {
                    mSurface = holder.getSurface();
                    configCodec(codec, format);
                }
            });

            return false;
        }

        return true;
    }

    public boolean initRender() {
        return true;
    }

    public void render(@NonNull ByteBuffer outputBuffer, @NonNull MediaCodec.BufferInfo bufferInfo) {

    }

    public void doneDecode() {
    }

    // $FF: synthetic method
    public static final Surface access$getMSurface$p(VideoDecoder $this) {
        return $this.mSurface;
    }
}
