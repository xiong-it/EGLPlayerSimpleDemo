package com.weidian.egldemo.media;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.weidian.egldemo.IDecoderStateListener;


/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public interface IDecoder extends Runnable {
    void pause();

    void goOn();

    long seekTo(long var1);

    long seekAndPlay(long var1);

    void stop();

    boolean isDecoding();

    boolean isSeeking();

    boolean isStop();

//    void setSizeListener(@NotNull IDecoderProgress var1);

    void setStateListener(@Nullable IDecoderStateListener var1);

    int getWidth();

    int getHeight();

    long getDuration();

    long getCurTimeStamp();

    int getRotationAngle();

    @Nullable
    MediaFormat getMediaFormat();

    int getTrack();

    @NonNull
    String getFilePath();

    @NonNull
    IDecoder withoutSync();
}

