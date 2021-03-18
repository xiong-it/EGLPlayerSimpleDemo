package com.weidian.egldemo.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.weidian.egldemo.IDecoderStateListener;


import java.io.File;
import java.nio.ByteBuffer;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public abstract class BaseDecoder implements IDecoder {
    private final String TAG = "BaseDecoder";
    private boolean mIsRunning;
    private final Object mLock;
    private boolean mReadyForDecode;
    private MediaCodec mCodec;
    private IExtractor mExtractor;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private MediaCodec.BufferInfo mBufferInfo;
    private DecodeState mState;
    @Nullable
    private IDecoderStateListener mStateListener;
    private boolean mIsEOS;
    private int mVideoWidth;
    private int mVideoHeight;
    private long mDuration;
    private long mStartPos;
    private long mEndPos;
    private long mStartTimeForSync = -1;
    private boolean mSyncRender;
    private final String mFilePath;

    @Nullable
    protected final IDecoderStateListener getMStateListener() {
        return this.mStateListener;
    }

    protected final void setMStateListener(@Nullable IDecoderStateListener var1) {
        this.mStateListener = var1;
    }

    protected final int getMVideoWidth() {
        return this.mVideoWidth;
    }

    protected final void setMVideoWidth(int var1) {
        this.mVideoWidth = var1;
    }

    protected final int getMVideoHeight() {
        return this.mVideoHeight;
    }

    protected final void setMVideoHeight(int var1) {
        this.mVideoHeight = var1;
    }

    @Override
    public final void run() {
        if (this.mState == DecodeState.STOP) {
            this.mState = DecodeState.START;
        }

        if (mStateListener != null) {
            mStateListener.decoderPrepare(this);
        }

        if (!this.init()) {
            return;
        }
        Log.e("TAG+++", "开始解码");

        try {
            while (this.mIsRunning) {
                if (this.mState != DecodeState.START && this.mState != DecodeState.DECODING && this.mState != DecodeState.SEEKING) {
                    Log.e("TAG+++", "进入等待：" + this.mState);
                    this.waitDecode();
                    this.mStartTimeForSync = System.currentTimeMillis() - this.getCurTimeStamp();
                }

                if (!this.mIsRunning || this.mState == DecodeState.STOP) {
                    this.mIsRunning = false;
                    break;
                }

                if (this.mStartTimeForSync == -1L) {
                    this.mStartTimeForSync = System.currentTimeMillis();
                }

                if (!this.mIsEOS) {
                    this.mIsEOS = this.pushBufferToDecoder();
                }

                int index = this.pullBufferFromDecoder();
                if (index >= 0) {
                    if (this.mSyncRender && this.mState == DecodeState.DECODING) {
                        this.sleepRender();
                    }

                    if (this.mSyncRender) {
                        if (mOutputBuffers == null) {
                            return;
                        }

                        this.render(mOutputBuffers[index], this.mBufferInfo);
                    }

                    if (mOutputBuffers == null) {
                        return;
                    }
                    Frame frame = new Frame();

                    frame.setBuffer(mOutputBuffers[index]);
                    frame.setBufferInfo(this.mBufferInfo);
                    if (mStateListener != null) {
                        mStateListener.decodeOneFrame(this, frame);
                    }

                    mCodec.releaseOutputBuffer(index, true);
                    if (this.mState == DecodeState.START) {
                        this.mState = DecodeState.PAUSE;
                    }
                }

                if (this.mBufferInfo.flags == 4) {
                    Log.i(this.TAG, "解码结束");
                    this.mState = DecodeState.FINISH;
                    if (mStateListener != null) {
                        mStateListener.decoderFinish(this);
                    }
                }
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        } finally {
            this.doneDecode();
            this.release();
        }
    }

    private final boolean init() {
        CharSequence var1 = (CharSequence) this.mFilePath;
        boolean var2 = false;
        if (var1.length() != 0 && (new File(this.mFilePath)).exists()) {
            if (!this.check()) {
                return false;
            } else {
                this.mExtractor = this.initExtractor(this.mFilePath);
                if (this.mExtractor != null) {
                    if (mExtractor == null) {
                        return false;
                    }

                    if (mExtractor.getFormat() != null) {
                        if (!this.initParams()) {
                            return false;
                        }

                        if (!this.initRender()) {
                            return false;
                        }

                        if (!this.initCodec()) {
                            return false;
                        }

                        return true;
                    }
                }

                Log.w(this.TAG, "无法解析文件");
                return false;
            }
        } else {
            Log.w(this.TAG, "文件路径为空");
            IDecoderStateListener var10000 = this.mStateListener;
            if (var10000 != null) {
                var10000.decoderError(this, "文件路径为空");
            }

            return false;
        }
    }

    private final boolean initParams() {
        try {
            if (mExtractor == null) {
                return false;
            }

            MediaFormat format = mExtractor.getFormat();
            if (format == null) {
                return false;
            }

            this.mDuration = format.getLong("durationUs") / (long) 1000;
            if (this.mEndPos == 0L) {
                this.mEndPos = this.mDuration;
            }

            if (mExtractor == null) {
                return false;
            }

            if (format == null) {
                return false;
            }

            this.initSpecParams(format);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }

    private final boolean initCodec() {
        try {
            if (mExtractor == null) {
                return false;
            }

            MediaFormat format = mExtractor.getFormat();
            if (format == null) {
                return false;
            }

            String type = format.getString("mime");
            this.mCodec = MediaCodec.createDecoderByType(type);
            MediaCodec var10001 = this.mCodec;
            if (mCodec == null) {
                return false;
            }

            if (!this.configCodec(mCodec, format)) {
                this.waitDecode();
            }

            mCodec.start();
            this.mInputBuffers = mCodec != null ? mCodec.getInputBuffers() : null;
            this.mOutputBuffers = mCodec != null ? mCodec.getOutputBuffers() : null;
            return true;
        } catch (Exception var2) {
            return false;
        }
    }

    private final boolean pushBufferToDecoder() {
        if (mCodec == null) {
            return false;
        }

        int inputBufferIndex = mCodec.dequeueInputBuffer(1000L);
        boolean isEndOfStream = false;
        if (inputBufferIndex >= 0) {

            ByteBuffer inputBuffer = mInputBuffers[inputBufferIndex];

            int sampleSize = mExtractor.readBuffer(inputBuffer);
            if (sampleSize < 0) {

                mCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, 4);
                isEndOfStream = true;
            } else {
                if (mCodec == null) {
                    return false;
                }

                if (mExtractor == null) {
                    return false;
                }

                mCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, mExtractor.getCurrentTimestamp(), 0);
            }
        }

        return isEndOfStream;
    }

    private final int pullBufferFromDecoder() {
        if (mCodec == null) {
            return 0;
        }

        int index = mCodec.dequeueOutputBuffer(this.mBufferInfo, 1000L);
        switch (index) {
            case -3:
                this.mOutputBuffers = mCodec.getOutputBuffers();
            case -2:
            case -1:
                return -1;
            default:
                return index;
        }
    }

    private final void sleepRender() {
        long passTime = System.currentTimeMillis() - this.mStartTimeForSync;
        long curTime = this.getCurTimeStamp();
        if (curTime > passTime) {
            try {
                Thread.sleep(curTime - passTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private final void release() {
        try {
            Log.i(this.TAG, "解码停止，释放解码器");
            this.mState = DecodeState.STOP;
            this.mIsEOS = false;
            if (mExtractor != null) {
                mExtractor.stop();
            }

            if (mCodec != null) {
                mCodec.stop();
            }

            if (mCodec != null) {
                mCodec.release();
            }

            if (mStateListener != null) {
                mStateListener.decoderDestroy(this);
            }
        } catch (Exception var2) {
        }

    }

    private final void waitDecode() {
        try {
            if (this.mState == DecodeState.PAUSE) {
                if (mStateListener != null) {
                    mStateListener.decoderPause(this);
                }
            }

            synchronized (mLock) {
                this.mLock.wait();
            }
        } catch (Exception var6) {
            var6.printStackTrace();
        }

    }

    protected final void notifyDecode() {
        synchronized (mLock) {
            this.mLock.notifyAll();
        }

        if (this.mState == DecodeState.DECODING) {
            if (mStateListener != null) {
                mStateListener.decoderRunning(this);
            }
        }

    }

    public void pause() {
        this.mState = DecodeState.PAUSE;
    }

    public void goOn() {
        this.mState = DecodeState.DECODING;
        this.notifyDecode();
        Log.e("TAG++", "goOn...");
    }

    public long seekTo(long pos) {
        return 0L;
    }

    public long seekAndPlay(long pos) {
        return 0L;
    }

    public void stop() {
        this.mState = DecodeState.STOP;
        this.mIsRunning = false;
        this.notifyDecode();
    }

    public boolean isDecoding() {
        return this.mState == DecodeState.DECODING;
    }

    public boolean isSeeking() {
        return this.mState == DecodeState.SEEKING;
    }

    public boolean isStop() {
        return this.mState == DecodeState.STOP;
    }

    public void setStateListener(@Nullable IDecoderStateListener l) {
        this.mStateListener = l;
    }

    public int getWidth() {
        return this.mVideoWidth;
    }

    public int getHeight() {
        return this.mVideoHeight;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public long getCurTimeStamp() {
        return this.mBufferInfo.presentationTimeUs / (long) 1000;
    }

    public int getRotationAngle() {
        return 0;
    }

    @Nullable
    public MediaFormat getMediaFormat() {
        return mExtractor != null ? mExtractor.getFormat() : null;
    }

    public int getTrack() {
        return 0;
    }

    @NonNull
    public String getFilePath() {
        return this.mFilePath;
    }

    @NonNull
    public IDecoder withoutSync() {
        this.mSyncRender = false;
        return (IDecoder) this;
    }

    public abstract boolean check();

    @NonNull
    public abstract IExtractor initExtractor(@NonNull String path);

    public abstract void initSpecParams(@NonNull MediaFormat format);

    public abstract boolean configCodec(@NonNull MediaCodec codec, @NonNull MediaFormat format);

    public abstract boolean initRender();

    public abstract void render(@NonNull ByteBuffer buffer, @NonNull MediaCodec.BufferInfo bufferInfo);

    public abstract void doneDecode();

    public BaseDecoder(@NonNull String mFilePath) {
        super();
        this.mFilePath = mFilePath;
        this.mIsRunning = true;
        this.mLock = new Object();
        this.mBufferInfo = new MediaCodec.BufferInfo();
        this.mState = DecodeState.STOP;
        this.mStartTimeForSync = -1L;
        this.mSyncRender = true;
    }
}