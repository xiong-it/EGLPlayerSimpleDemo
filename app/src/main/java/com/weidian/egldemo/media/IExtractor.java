package com.weidian.egldemo.media;

import android.media.MediaFormat;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public interface IExtractor {
    @Nullable
    MediaFormat getFormat();

    int readBuffer(@NonNull ByteBuffer buffer);

    long getCurrentTimestamp();

    int getSampleFlag();

    long seek(long pos);

    void setStartPos(long pos);

    void stop();
}
