package com.kiwi.ui;

import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.bean.conf.StickerConfig;

import java.util.List;

/**
 * Created by shijian on 08/12/2016.
 */

public interface OnViewEventListener {
    void onTakeShutter();

    void onSwitchCamera();

    void onFilterChanged(KwFilterType filterType);

    void onStickerChanged(StickerConfig stickerConfig);

    void onSwitchBeauty(boolean enable);

    void onSwitchBeautyFace(boolean enable);

    void onSwitchDrawPoints();

    void onDistortionChanged(KwFilterType filterType);

    void onAdjustFaceBeauty(int type, float param);

    void onFaceBeautyLevel(float level);

    void onSwitchBeauty2(boolean enable);

    List<StickerConfig> getStickers();

    List<StickerConfig> getGiftStickers();

    void writeSticker(StickerConfig stickerConfig);

    void onGiveGift(StickerConfig giftSticker);
}