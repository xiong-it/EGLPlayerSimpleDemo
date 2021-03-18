package com.weidian.egldemo.media;

import android.graphics.SurfaceTexture;

import com.weidian.egldemo.oepngl.IDrawer;
import com.weidian.egldemo.oepngl.VideoDrawer;

/**
 * @description: 文件描述
 * @author: xiongxunxiang
 * @date: 2021/3/18
 */
public class TranstionDrawer extends VideoDrawer {
    private int mTextureId = -1;

    @Override
    public void draw() {

    }

    @Override
    public void setTextureID(int id) {
        mTextureId = id;
    }

    @Override
    public void release() {

    }
}
