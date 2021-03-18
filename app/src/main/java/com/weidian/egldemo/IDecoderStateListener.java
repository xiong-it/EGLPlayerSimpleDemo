package com.weidian.egldemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.weidian.egldemo.media.BaseDecoder;
import com.weidian.egldemo.media.Frame;


/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public interface IDecoderStateListener {
    void decoderPrepare(@Nullable BaseDecoder decoder);

    void decoderReady(@Nullable BaseDecoder decoder);

    void decoderRunning(@Nullable BaseDecoder decoder);

    void decoderPause(@Nullable BaseDecoder decoder);

    void decodeOneFrame(@Nullable BaseDecoder decoder, @NonNull Frame frame);

    void decoderFinish(@Nullable BaseDecoder decoder);

    void decoderDestroy(@Nullable BaseDecoder decoder);

    void decoderError(@Nullable BaseDecoder decoder, @NonNull String msg);
}
