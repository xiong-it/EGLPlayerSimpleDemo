package com.weidian.egldemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.weidian.egldemo.media.AudioDecoder;
import com.weidian.egldemo.media.BaseDecoder;
import com.weidian.egldemo.media.Frame;
import com.weidian.egldemo.media.VideoDecoder;
import com.weidian.egldemo.oepngl.CustomerGLRenderer;
import com.weidian.egldemo.oepngl.IDrawer;
import com.weidian.egldemo.oepngl.VideoDrawer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends AppCompatActivity {
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mvtest.mp4";
    private CustomerGLRenderer render;
    private ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    SurfaceView videoView;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        render = new CustomerGLRenderer();

        videoView = findViewById(R.id.video_view);
        initVideo();
        setRenderSurface();
    }

    private void initVideo() {
        VideoDrawer drawer = new VideoDrawer();
        drawer.setAlpha(1.f);
        drawer.setVideoSize(1920, 1080);
        render.addDrawer(drawer);
        drawer.getSurfaceTexture(new VideoDrawer.OnSurfaceTextureListener() {
            @Override
            public void onSurfaceTextureCreate(SurfaceTexture texture) {
                initPlayer(texture);
            }
        });

    }

    private void initPlayer(SurfaceTexture texture) {
        VideoDecoder decoder = new VideoDecoder(path, null, new Surface(texture));
        threadPool.submit(decoder);
        decoder.goOn();
        decoder.setStateListener(new IDecoderStateListener() {
            @Override
            public void decoderPrepare(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decoderReady(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decoderRunning(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decoderPause(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decodeOneFrame(@Nullable BaseDecoder decoder, @NonNull Frame frame) {
                render.notifySwap(frame.getBufferInfo().presentationTimeUs);
            }

            @Override
            public void decoderFinish(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decoderDestroy(@Nullable BaseDecoder decoder) {

            }

            @Override
            public void decoderError(@Nullable BaseDecoder decoder, @NonNull String msg) {

            }
        });

        AudioDecoder audioDecoder = new AudioDecoder(path);
        threadPool.submit(audioDecoder);
        audioDecoder.goOn();
    }

    private void setRenderSurface() {
        render.setSurface(videoView);
    }
}