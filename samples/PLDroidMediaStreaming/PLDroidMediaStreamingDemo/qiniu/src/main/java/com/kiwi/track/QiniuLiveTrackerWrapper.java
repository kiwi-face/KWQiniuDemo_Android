package com.kiwi.track;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.kiwi.sticker.StickerMgr;
import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerContext;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.ui.OnViewEventListener;

import java.util.List;

import static com.kiwi.tracker.common.Config.isDebug;
import static com.kiwi.ui.KwControlView.BEAUTY_BIG_EYE_TYPE;
import static com.kiwi.ui.KwControlView.BEAUTY_THIN_FACE_TYPE;
import static com.kiwi.ui.KwControlView.REMOVE_BLEMISHES;
import static com.kiwi.ui.KwControlView.SKIN_SHINNING_TENDERNESS;
import static com.kiwi.ui.KwControlView.SKIN_TONE_PERFECTION;
import static com.kiwi.ui.KwControlView.SKIN_TONE_SATURATION;

/**
 * TODO:请关掉七牛直播自带美颜，接口类
 * 实现（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
 * Created by shijian on 2016/9/28.
 */

public class QiniuLiveTrackerWrapper {
    private static final String TAG = QiniuLiveTrackerWrapper.class.getName();

    private KwTrackerSettings kwTrackerSetting;
    private KwTrackerManager kwTrackerManager;
    private KwTrackerContext kwTrackerContext = new KwTrackerContext();

    public QiniuLiveTrackerWrapper(Context context, int cameraFaceId) {

        kwTrackerSetting = new KwTrackerSettings().
                setBeautyEnabled(true).
                setCameraFaceId(cameraFaceId);

        kwTrackerManager = new KwTrackerManager(context).
                setTrackerSetting(kwTrackerSetting).
                setStickerMgr(new StickerMgr())
                .setTrackerContext(kwTrackerContext)
                .build();

        initKiwiConfig();
    }

    private void initKiwiConfig() {
        //推荐配置，以下情况，选择性能优先模式
        //1.oppo vivo你懂的
        //2.小于5.0的机型可能配置比较差
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Log.i(TAG,String.format("manufacturer:%s,model:%s,sdk:%s",manufacturer,Build.MODEL,Build.VERSION.SDK_INT));
        boolean isOppoVivo = manufacturer.contains("oppo") || manufacturer.contains("vivo");
//        if(isOppoVivo || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            Config.TRACK_MODE = Config.TRACK_PRIORITY_PERFORMANCE;
        }

        //关闭日志打印,release版本请务必关闭日志打印
        Config.isDebug = true;
    }


    public void onCreate(Activity activity) {
        kwTrackerContext.onCreate(activity);}

    public void onResume(Activity activity) {
        kwTrackerContext.onResume(activity);
    }

    public void onPause(Activity activity) {
        kwTrackerContext.onPause(activity);
    }

    public void onDestroy(Activity activity) {
        kwTrackerContext.onDestory(activity);
    }

    public void onSurfaceCreated(Context context) {
        kwTrackerContext.onSurfaceCreated(context);
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        kwTrackerContext.onSurfaceChanged(width, height, previewWidth, previewHeight);
    }

    public void onSurfaceDestroyed() {
        kwTrackerContext.onSurfaceDestroyed();
    }

    public void switchCamera(int ordinal) {
        kwTrackerManager.switchCamera(ordinal);
    }

    /**
     * 对纹理进行特效处理（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
     * @param texId YUV格式纹理
     * @param texWidth 纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrame(int texId, int texWidth, int texHeight) {
        long start = System.currentTimeMillis();

        //解开绑定
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        int newTexId = texId;
        int maxFaceCount = 1;
        int filterTexId = kwTrackerManager.onDrawOESTexture(texId, texWidth, texHeight, maxFaceCount);
        if (filterTexId != -1) {
            newTexId = filterTexId;
        }

        int error = GLES20.glGetError();//请勿删除当前行获取opengl错误代码
        if(error != GLES20.GL_NO_ERROR){
            Log.d("Tracker","glError:"+error);
        }

        if (isDebug)
            Log.i(TAG, "[end][succ]onDrawFrame,cost:" + (System.currentTimeMillis() - start)+",in:"+texId+",out:"+newTexId);

        return newTexId;
    }

    /**
     * UI事件处理类
     * @param switchCamearRunnable
     * @return
     */
    public OnViewEventListener initUIEventListener(final Runnable switchCamearRunnable)
    {
        OnViewEventListener eventListener = new OnViewEventListener() {

            @Override
            public void onSwitchBeauty2(boolean enable) {
                getKwTrackerManager().setBeauty2Enabled(enable);
            }

            @Override
            public void onTakeShutter() {

            }

            @Override
            public void onSwitchCamera() {
                switchCamearRunnable.run();
            }

            @Override
            public void onFilterChanged(KwFilterType filterType) {
                getKwTrackerManager().switchFilter(filterType);
            }

            @Override
            public void onStickerChanged(StickerConfig item) {
                getKwTrackerManager().switchSticker(item);
            }

            @Override
            public void onSwitchBeauty(boolean enable) {
                getKwTrackerManager().setBeautyEnabled(enable);
            }

            @Override
            public void onSwitchBeautyFace(boolean enable) {
                getKwTrackerManager().setBeautyFaceEnabled(enable);
            }

            @Override
            public void onSwitchDrawPoints() {
                getKwTrackerManager().switchDrawPoints();
            }

            @Override
            public void onDistortionChanged(KwFilterType filterType) {
                getKwTrackerManager().switchDistortion(filterType);

            }

            @Override
            public void onFaceBeautyLevel(float level) {
                getKwTrackerManager().adjustBeauty(level);
            }

            @Override
            public void onGiveGift(StickerConfig giftSticker) {
                getKwTrackerManager().switchGift(giftSticker, 1);
            }

            @Override
            public List<StickerConfig> getGiftStickers() {
                return getKwTrackerManager().getGiftStickerMgr().getStickerConfigs();
            }

            @Override
            public void onAdjustFaceBeauty(int type, float param) {
                switch (type) {
                    case BEAUTY_BIG_EYE_TYPE:
                        getKwTrackerManager().adjustFaceBigEyeScale(param);
                        break;
                    case BEAUTY_THIN_FACE_TYPE:
                        getKwTrackerManager().adjustFaceThinFaceScale(param);
                        break;
                    case SKIN_SHINNING_TENDERNESS:
                        getKwTrackerManager().adjustSkinShinningTenderness(param);
                        break;
                    case SKIN_TONE_SATURATION:
                        getKwTrackerManager().adjustSkinToneSaturation(param);
                        break;
                    case REMOVE_BLEMISHES:
                        getKwTrackerManager().adjustRemoveBlemishes(param);
                        break;
                    case SKIN_TONE_PERFECTION:
                        getKwTrackerManager().adjustSkinTonePerfection(param);
                        break;
                }

            }

            @Override
            public List<StickerConfig> getStickers() {
                return getKwTrackerManager().getStickerMgr().getStickerConfigs();
            }

            @Override
            public void writeSticker(StickerConfig stickerConfig) {
                getKwTrackerManager().getStickerMgr().updateStickerConfig(stickerConfig);
            }

            private KwTrackerManager getKwTrackerManager() {
                return kwTrackerManager;
            }
        };

        return eventListener;
    }



}
