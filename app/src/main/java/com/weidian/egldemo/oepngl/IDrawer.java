package com.weidian.egldemo.oepngl;

import android.graphics.SurfaceTexture;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/17
 */
public interface IDrawer {
    void setVideoSize(int videoW , int videoH);
    void setWorldSize(int worldW, int worldH);
    void setAlpha(Float alpha);
    void draw();
    void setTextureID(int id);
    void release();
}
