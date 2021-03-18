package com.weidian.egldemo.media;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @description: 音视频分离器
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class MMExtractor {
    private MediaExtractor mExtractor = new MediaExtractor();
    private int mAudioTrack = -1;
    private int mVideoTrack = -1;
    private long mCurSampleTime;
    private int mCurSampleFlag;
    private long mStartPos;

    @Nullable
    public final MediaFormat getVideoFormat() {
        int i = 0;
        MediaExtractor var10000 = this.mExtractor;
        if (mExtractor == null) {
            return null;
        }

        for(int var2 = mExtractor.getTrackCount(); i < var2; ++i) {
            MediaFormat mediaFormat = var10000.getTrackFormat(i);
            String mime = mediaFormat.getString("mime");
            if (mime.startsWith("video/")) {
                this.mVideoTrack = i;
                break;
            }
        }

        MediaFormat format;
        if (this.mVideoTrack >= 0) {
            format = mExtractor.getTrackFormat(this.mVideoTrack);
        } else {
            format = null;
        }

        return format;
    }

    @Nullable
    public final MediaFormat getAudioFormat() {
        int i = 0;
        MediaExtractor var10000 = this.mExtractor;
        if (mExtractor == null) {
            return null;
        }

        for(int var2 = var10000.getTrackCount(); i < var2; ++i) {
            MediaFormat mediaFormat = var10000.getTrackFormat(i);
            String mime = mediaFormat.getString("mime");
            if (mime.startsWith("audio/")) {
                this.mAudioTrack = i;
                break;
            }
        }

        MediaFormat format;
        if (this.mAudioTrack >= 0) {
            format = var10000.getTrackFormat(this.mAudioTrack);
        } else {
            format = null;
        }

        return format;
    }

    public final int readBuffer(@NonNull ByteBuffer byteBuffer) {
        byteBuffer.clear();
        this.selectSourceTrack();

        int readSampleCount = mExtractor.readSampleData(byteBuffer, 0);
        if (readSampleCount < 0) {
            return -1;
        } else {
            this.mCurSampleTime = mExtractor.getSampleTime();
            this.mCurSampleFlag = mExtractor.getSampleFlags();
            mExtractor.advance();
            return readSampleCount;
        }
    }

    private final void selectSourceTrack() {
        if (this.mVideoTrack >= 0) {
            if (mExtractor == null) {
                return;
            }
            mExtractor.selectTrack(this.mVideoTrack);
        } else if (this.mAudioTrack >= 0) {
            mExtractor.selectTrack(this.mAudioTrack);
        }

    }

    public final long seek(long pos) {
        if (mExtractor == null) {
            return 0;
        }

        mExtractor.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        return mExtractor.getSampleTime();
    }

    public final void stop() {
        MediaExtractor var10000 = this.mExtractor;
        if (var10000 != null) {
            var10000.release();
        }

        this.mExtractor = (MediaExtractor)null;
    }

    public final int getVideoTrack() {
        return this.mVideoTrack;
    }

    public final int getAudioTrack() {
        return this.mAudioTrack;
    }

    public final void setStartPos(long pos) {
        this.mStartPos = pos;
    }

    public final long getCurrentTimestamp() {
        return this.mCurSampleTime;
    }

    public final int getSampleFlag() {
        return this.mCurSampleFlag;
    }

    public MMExtractor(@Nullable String path) {
        if (mExtractor != null) {
            try {
                mExtractor.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
