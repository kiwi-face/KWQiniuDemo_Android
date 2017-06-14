package com.qiniu.pili.droid.shortvideo.demo.tracker;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.kiwi.tracker.KwFaceTracker;
import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.KwFilter;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.ui.OnViewEventListener;
import com.kiwi.ui.helper.ResourceHelper;
import com.kiwi.ui.model.SharePreferenceMgr;

import static com.kiwi.tracker.common.Config.isDebug;
import static com.kiwi.ui.KwControlView.BEAUTY_BIG_EYE_TYPE;
import static com.kiwi.ui.KwControlView.BEAUTY_THIN_FACE_TYPE;
import static com.kiwi.ui.KwControlView.REMOVE_BLEMISHES;
import static com.kiwi.ui.KwControlView.SKIN_SHINNING_TENDERNESS;
import static com.kiwi.ui.KwControlView.SKIN_TONE_PERFECTION;
import static com.kiwi.ui.KwControlView.SKIN_TONE_SATURATION;

/**
 * fixme:请关掉七牛直播自带美颜，接口类,所有对kiwi face sdk的调用都封装在这个类里面
 * 实现（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
 * Created by shijian on 2016/9/28.
 */
public class QiniuLiveTrackerWrapper {
    private static final String TAG = QiniuLiveTrackerWrapper.class.getName();

    private KwTrackerSettings kwTrackerSetting;
    private KwTrackerManager kwTrackerManager;

    public QiniuLiveTrackerWrapper(Context context, int cameraFaceId) {

//        kwTrackerSetting = new KwTrackerSettings().
//                setBeautyEnabled(true).
//                setCameraFaceId(cameraFaceId)
//                .setBeauty2Enabled(true);
//
//        kwTrackerManager = new KwTrackerManager(context).
//                setTrackerSetting(kwTrackerSetting).
//                setStickerMgr(new StickerMgr())
//                .build();

        SharePreferenceMgr instance = SharePreferenceMgr.getInstance();

        KwTrackerSettings.BeautySettings2 beautySettings2 = new KwTrackerSettings.BeautySettings2();
        beautySettings2.setWhiteProgress(instance.getSkinWhite());
        beautySettings2.setDermabrasionProgress(instance.getSkinRemoveBlemishes());
        beautySettings2.setSaturatedProgress(instance.getSkinSaturation());
        beautySettings2.setPinkProgress(instance.getSkinTenderness());

        KwTrackerSettings.BeautySettings beautySettings = new KwTrackerSettings.BeautySettings();
        beautySettings.setBigEyeScaleProgress(instance.getBigEye());
        beautySettings.setThinFaceScaleProgress(instance.getThinFace());

        kwTrackerSetting = new KwTrackerSettings().
                setBeauty2Enabled(instance.isBeautyEnabled()).
                setBeautySettings2(beautySettings2).
                setBeautyFaceEnabled(instance.isLocalBeautyEnabled()).
                setBeautySettings(beautySettings).
                setCameraFaceId(cameraFaceId)
                .setDefaultDirection(KwFaceTracker.CV_CLOCKWISE_ROTATE_180);

        kwTrackerManager = new KwTrackerManager(context).
                setTrackerSetting(kwTrackerSetting).
                build();

        //copy assets config/sticker/filter to sdcard
        ResourceHelper.copyResource2SD(context);

        initKiwiConfig();
    }

    private void initKiwiConfig() {
        //推荐配置，以下情况，选择性能优先模式
        //1.oppo vivo你懂的
        //2.小于5.0的机型可能配置比较差
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Log.i(TAG, String.format("manufacturer:%s,model:%s,sdk:%s", manufacturer, Build.MODEL, Build.VERSION.SDK_INT));
        boolean isOppoVivo = manufacturer.contains("oppo") || manufacturer.contains("vivo");
        if (isOppoVivo || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Config.TRACK_MODE = Config.TRACK_PRIORITY_PERFORMANCE;
        }

        //关闭日志打印,release版本请务必关闭日志打印
        Config.isDebug = true;
        Config.outPutTestBitmap = true;

        //TODO
    }

    public void onCreate(Activity activity) {
        kwTrackerManager.onCreate(activity);
    }

    public void onResume(Activity activity) {
        kwTrackerManager.onResume(activity);
    }

    public void onPause(Activity activity) {
        kwTrackerManager.onPause(activity);
    }

    public void onDestroy(Activity activity) {
        kwTrackerManager.onDestory(activity);
    }

    public void onSurfaceCreated(Context context) {
        kwTrackerManager.onSurfaceCreated(context);
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        kwTrackerManager.onSurfaceChanged(width, height, previewWidth, previewHeight);
    }

    public void onSurfaceDestroyed() {
        kwTrackerManager.onSurfaceDestroyed();
    }

    public void switchCamera(int ordinal) {
        kwTrackerManager.switchCamera(ordinal);
    }

    /**
     * 对纹理进行特效处理（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrame(int texId, int texWidth, int texHeight) {
        long start = System.currentTimeMillis();

        Log.e("tracker", "isNeedTrack:" + kwTrackerSetting.isNeedTrack());

        Log.e(TAG, "width" + texWidth + "-----height" + texHeight);

        int newTexId = texId;
        int maxFaceCount = 1;
        int filterTexId = kwTrackerManager.onDrawTexture2D(texId, texWidth, texHeight, maxFaceCount);
        if (filterTexId != -1) {
            newTexId = filterTexId;
        }

        int error = GLES20.glGetError();//请勿删除当前行获取opengl错误代码
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("Tracker", "glError:" + error);
        }

        if (isDebug)
            Log.i(TAG, "[end][succ]onDrawFrame,cost:" + (System.currentTimeMillis() - start) + ",in:" + texId + ",out:" + newTexId);

        return newTexId;
    }

    /**
     * UI事件处理类
     *
     * @param switchCameraRunnable
     * @return
     */
    public OnViewEventListener initUIEventListener(final Runnable switchCameraRunnable) {
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
                switchCameraRunnable.run();
            }

            @Override
            public void onSwitchFilter(KwFilter filter) {
                getKwTrackerManager().switchFilter(filter);
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
            public void onDistortionChanged(KwFilterType filterType) {
                getKwTrackerManager().switchDistortion(filterType);

            }

            @Override
            public void onFaceBeautyLevel(float level) {
                getKwTrackerManager().adjustBeauty(level);
            }

            @Override
            public void onAdjustFaceBeauty(int type, float param) {
                switch (type) {
                    case BEAUTY_BIG_EYE_TYPE:
                        getKwTrackerManager().setEyeMagnifying((int) param);
                        break;
                    case BEAUTY_THIN_FACE_TYPE:
                        getKwTrackerManager().setChinSliming((int) param);
                        break;
                    case SKIN_SHINNING_TENDERNESS:
                        getKwTrackerManager().setSkinTenderness((int) param);
                        break;
                    case SKIN_TONE_SATURATION:
                        getKwTrackerManager().setSkinSaturation((int) param);
                        break;
                    case REMOVE_BLEMISHES:
                        getKwTrackerManager().setSkinBlemishRemoval((int) param);
                        break;
                    case SKIN_TONE_PERFECTION:
                        getKwTrackerManager().setSkinWhitening((int) param);
                        break;
                }

            }

            private KwTrackerManager getKwTrackerManager() {
                return kwTrackerManager;
            }
        };

        return eventListener;
    }

}
