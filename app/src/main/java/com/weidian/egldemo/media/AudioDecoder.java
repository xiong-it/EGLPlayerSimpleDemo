package com.weidian.egldemo.media;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class AudioDecoder extends BaseDecoder {
    private int mSampleRate;
    private int mChannels;
    private int mPCMEncodeBit;
    private AudioTrack mAudioTrack;
    private short[] mAudioOutTempBuf;

    public boolean check() {
        return true;
    }

    @NonNull
    public IExtractor initExtractor(@NonNull String path) {
        return (IExtractor) (new AudioExtractor(path));
    }

    public void initSpecParams(@NonNull MediaFormat format) {
        try {
            this.mChannels = format.getInteger("channel-count");
            this.mSampleRate = format.getInteger("sample-rate");
            this.mPCMEncodeBit = format.containsKey("pcm-encoding") ? format.getInteger("pcm-encoding") : 2;
        } catch (Exception var3) {
        }

    }

    public boolean configCodec(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
        codec.configure(format, (Surface) null, (MediaCrypto) null, 0);
        return true;
    }

    public boolean initRender() {
        int channel = this.mChannels == 1 ? 4 : 12;
        int minBufferSize = AudioTrack.getMinBufferSize(this.mSampleRate, channel, this.mPCMEncodeBit);
        this.mAudioOutTempBuf = new short[minBufferSize / 2];
        this.mAudioTrack = new AudioTrack(3, this.mSampleRate, channel, this.mPCMEncodeBit, minBufferSize, 1);
        if (mAudioTrack == null) {
            return false;
        }

        mAudioTrack.play();
        return true;
    }

    public void render(@NonNull ByteBuffer outputBuffer, @NonNull MediaCodec.BufferInfo bufferInfo) {
        if (mAudioOutTempBuf == null) {
            return;
        }

        if (mAudioOutTempBuf.length < bufferInfo.size / 2) {
            this.mAudioOutTempBuf = new short[bufferInfo.size / 2];
        }

        outputBuffer.position(0);
        outputBuffer.asShortBuffer().get(this.mAudioOutTempBuf, 0, bufferInfo.size / 2);
        AudioTrack var3 = this.mAudioTrack;
        if (mAudioTrack == null) {
            return;
        }
        mAudioTrack.write(mAudioOutTempBuf, 0, bufferInfo.size / 2);
    }

    public void doneDecode() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

    }

    public AudioDecoder(@NonNull String path) {
        super(path);
        this.mSampleRate = -1;
        this.mChannels = 1;
        this.mPCMEncodeBit = 2;
    }
}
