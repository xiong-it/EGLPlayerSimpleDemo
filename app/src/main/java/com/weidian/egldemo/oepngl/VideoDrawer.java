package com.weidian.egldemo.oepngl;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @description: 视频绘制
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public class VideoDrawer implements IDrawer {
    protected static final String VertexShader =
            "attribute vec4 aPosition;" +
                    "precision mediump float;" +
                    "uniform mat4 uMatrix;" +
                    "attribute vec2 aCoordinate;" +
                    "varying vec2 vCoordinate;" +
                    "attribute float alpha;" +
                    "varying float inAlpha;" +
                    "void main() {" +
                    "    gl_Position = uMatrix*aPosition;" +
                    "    vCoordinate = aCoordinate;" +
                    "    inAlpha = alpha;" +
                    "}";


    protected static final String FragmentShader =
            //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 vCoordinate;" +
                    "varying float inAlpha;" +
                    "uniform samplerExternalOES uTexture;" +
                    "void main() {" +
                    "  vec4 color = texture2D(uTexture, vCoordinate);" +
                    "  gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
                    "}";

    protected static final float mVertexCoors[] = new float[]{
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };
    protected static final float mTextureCoors[] = new float[]{
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    protected int mWorldWidth = -1;
    protected int mWorldHeight = -1;
    protected int mVideoWidth = -1;
    protected int mVideoHeight = -1;
    protected int mTextureId = -1;
    protected SurfaceTexture mSurfaceTexture;
    protected int mProgram = -1;
    protected int mVertexMatrixHandler;
    protected int mVertexPosHandler;
    protected int mTextureHandler;
    protected int mTexturePosHandler;
    protected int mAlphaHandler;
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;
    protected float[] mMatrix;
    protected float mAlpha = 1f;
    protected float mWidthRatio = 1f;
    protected float mHeightRatio = 1f;

    private OnSurfaceTextureListener listener;

    public VideoDrawer() {
        //【步骤1: 初始化顶点坐标】
        initPos();
    }

    private void initPos() {
        ByteBuffer bb = ByteBuffer.allocateDirect(mVertexCoors.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mVertexCoors);
        mVertexBuffer.position(0);

        ByteBuffer cc = ByteBuffer.allocateDirect(mTextureCoors.length * 4);
        cc.order(ByteOrder.nativeOrder());
        mTextureBuffer = cc.asFloatBuffer();
        mTextureBuffer.put(mTextureCoors);
        mTextureBuffer.position(0);
    }

    private void initDefMatrix() {
        if (mMatrix != null) {
            return;
        }

        if (mVideoWidth != -1 && mVideoHeight != -1
                && mWorldWidth != -1 && mWorldHeight != -1) {
            mMatrix = new float[16];
            float prjMatrix[] = new float[16];

            float originRatio = mVideoWidth / (float) mVideoHeight;
            float worldRatio = mWorldWidth / (float) mWorldHeight;
            if (mWorldHeight > mWorldHeight) {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio;
                    Matrix.orthoM(prjMatrix, 0, -mWidthRatio, mWidthRatio, -mHeightRatio, mHeightRatio, 3, 5);
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio;
                    Matrix.orthoM(
                            prjMatrix, 0,
                            -mWidthRatio, mWidthRatio,
                            -mHeightRatio, mHeightRatio,
                            3f, 5f
                    );
                }
            } else {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio;
                    Matrix.orthoM(
                            prjMatrix, 0,
                            -mWidthRatio, mWidthRatio,
                            -mHeightRatio, mHeightRatio,
                            3f, 5f
                    );
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio;
                    Matrix.orthoM(
                            prjMatrix, 0,
                            -mWidthRatio, mWidthRatio,
                            -mHeightRatio, mHeightRatio,
                            3f, 5f
                    );
                }
            }
            //设置相机位置
            float viewMatrix[] = new float[16];
            Matrix.setLookAtM(
                    viewMatrix, 0,
                    0f, 0f, 5.0f,
                    0f, 0f, 0f,
                    0f, 1.0f, 0f
            );
            //计算变换矩阵
            Matrix.multiplyMM(mMatrix, 0, prjMatrix, 0, viewMatrix, 0);
        }
    }


    public void setVideoSize(int videoW, int videoH) {
        mVideoWidth = videoW;
        mVideoHeight = videoH;
    }

    public void setWorldSize(int worldW, int worldH) {
        mWorldWidth = worldW;
        mWorldHeight = worldH;
    }

    public void setAlpha(Float alpha) {
        mAlpha = alpha;
    }

    public void setTextureID(int id) {
        mTextureId = id;
        mSurfaceTexture = new SurfaceTexture(id);
        if (listener != null) {
            listener.onSurfaceTextureCreate(mSurfaceTexture);
        }
    }

    @Override
    public void release() {
        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTexturePosHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
        GLES20.glDeleteProgram(mProgram);
    }

    public void getSurfaceTexture(OnSurfaceTextureListener listener) {
        this.listener = listener;
    }

    public void draw() {
        if (mTextureId != -1) {
            initDefMatrix();
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg();
            //【步骤3: 激活并绑定纹理单元】
            activateTexture();
            //【步骤4: 绑定图片到纹理单元】
            updateTexture();
            //【步骤5: 开始渲染绘制】
            doDraw();
        }
    }

    private void createGLPrg() {
        if (mProgram == -1) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShader);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShader);

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES20.glCreateProgram();
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader);
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShader);
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram);

            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix");
            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture");
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
            mAlphaHandler = GLES20.glGetAttribLocation(mProgram, "alpha");
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram);
    }

    private int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES20.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    protected void activateTexture() {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(mTextureHandler, 0);
        //配置边缘过渡参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void updateTexture() {
        mSurfaceTexture.updateTexImage();
    }

    private void doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTexturePosHandler);
        GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0);
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glVertexAttrib1f(mAlphaHandler, mAlpha);
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public interface OnSurfaceTextureListener {
        void onSurfaceTextureCreate(SurfaceTexture texture);
    }
}
