package com.kiwi.ui.bean;

/**
 * Created by shijian on 2016/12/2.
 */
public class StickerInfo {
    private int thumbResId;
    private int textResId;
    private int colorResId;

    public StickerInfo(int thumbResId, int textResId, int colorResId) {
        this.thumbResId = thumbResId;
        this.textResId = textResId;
        this.colorResId = colorResId;
    }

    public int getThumbResId() {
        return thumbResId;
    }

    public int getTextResId() {
        return textResId;
    }

    public int getColorResId() {
        return colorResId;
    }
}
