package com.qiniu.pili.droid.shortvideo.demo.utils;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.kiwi.filter.utils.TextureUtils;
import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.KwFilter;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.tracker.fbo.RotateFBO;
import com.kiwi.tracker.utils.Accelerometer;
import com.kiwi.tracker.utils.FTCameraUtils;
import com.kiwi.tracker.utils.TrackerConstant;
import com.kiwi.ui.OnViewEventListener;
import com.kiwi.ui.helper.ResourceHelper;
import com.kiwi.ui.model.SharePreferenceMgr;

import static com.kiwi.ui.KwControlView.BEAUTY_BIG_EYE_TYPE;
import static com.kiwi.ui.KwControlView.BEAUTY_THIN_FACE_TYPE;
import static com.kiwi.ui.KwControlView.REMOVE_BLEMISHES;
import static com.kiwi.ui.KwControlView.SKIN_SHINNING_TENDERNESS;
import static com.kiwi.ui.KwControlView.SKIN_TONE_PERFECTION;
import static com.kiwi.ui.KwControlView.SKIN_TONE_SATURATION;


public class KiwiTrackWrapper {
    RotateFBO mRotateFBO;
    int mFirstDir,mLastDir;
    private int mCameraId;
    private static final String TAG = KiwiTrackWrapper.class.getName();

    public interface UIClickListener {
        void onTakeShutter();

        void onSwitchCamera();
    }

    private KwTrackerSettings mTrackerSetting;
    private KwTrackerManager mTrackerManager;

    public KiwiTrackWrapper(final Context context, int cameraFaceId) {

        SharePreferenceMgr instance = SharePreferenceMgr.getInstance();

        KwTrackerSettings.BeautySettings2 beautySettings2 = new KwTrackerSettings.BeautySettings2();
        beautySettings2.setWhiteProgress(instance.getSkinWhite());
        beautySettings2.setDermabrasionProgress(instance.getSkinRemoveBlemishes());
        beautySettings2.setSaturatedProgress(instance.getSkinSaturation());
        beautySettings2.setPinkProgress(instance.getSkinTenderness());

        KwTrackerSettings.BeautySettings beautySettings = new KwTrackerSettings.BeautySettings();
        beautySettings.setBigEyeScaleProgress(instance.getBigEye());
        beautySettings.setThinFaceScaleProgress(instance.getThinFace());

        mTrackerSetting = new KwTrackerSettings().
                setBeauty2Enabled(instance.isBeautyEnabled()).
                setBeautySettings2(beautySettings2).
                setBeautyFaceEnabled(instance.isLocalBeautyEnabled()).
                setBeautySettings(beautySettings).
                setCameraFaceId(cameraFaceId);

        mTrackerManager = new KwTrackerManager(context).
                setTrackerSetting(mTrackerSetting)
                .build();

        //copy assets config/sticker/filter to sdcard
        ResourceHelper.copyResource2SD(context);

        initKiwiConfig();
        mCameraId = cameraFaceId % 2;
        setConfigShortVideo(cameraFaceId);
    }


    private void setConfigShortVideo(int cameraFaceId){
        TextureUtils.setInverted(true);
        int cameraDegree = FTCameraUtils.getOrientation(Config.getContext(), cameraFaceId);
        switch (cameraDegree){
            case 0:
                TextureUtils.setDir(TextureUtils.DIR_0);
                break;
            case 90:
                TextureUtils.setDir(TextureUtils.DIR_90);
                break;
            case 180:
                TextureUtils.setDir(TextureUtils.DIR_180);
                break;
            case 270:
                TextureUtils.setDir(TextureUtils.DIR_270);
                break;
        }
    }

    private void initKiwiConfig() {
        //推荐配置，以下情况，选择性能优先模式
        //1.oppo vivo你懂的
        //2.小于5.0的机型可能配置比较差
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Log.i(TAG, String.format("manufacturer:%s,model:%s,sdk:%s", manufacturer, Build.MODEL, Build.VERSION.SDK_INT));
        boolean isOppoVivo = manufacturer.contains("oppo") || manufacturer.contains("vivo");
        Log.i(TAG, "initKiwiConfig buildVersion" + Build.VERSION.RELEASE);
        if (isOppoVivo || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Config.TRACK_MODE = Config.TRACK_PRIORITY_PERFORMANCE;
        }

        //关闭日志打印,release版本请务必关闭日志打印
        Config.isDebug = true;
        TrackerConstant.DEBUG = true;
    }

    public void onCreate(Activity activity) {
        mTrackerManager.onCreate(activity);
        mRotateFBO = new RotateFBO(GLES20.GL_TEXTURE_2D);
        mRotateFBO.onCreate(activity);

    }

    public void onResume(Activity activity) {
        mTrackerManager.onResume(activity);
    }

    public void onPause(Activity activity) {
        mTrackerManager.onPause(activity);
    }

    public void onDestroy(Activity activity) {
        mTrackerManager.onDestory(activity);
    }

    public void onSurfaceCreated(Context context) {
        mTrackerManager.onSurfaceCreated(context);
        mRotateFBO.initialize(context);
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        mTrackerManager.onSurfaceChanged(width, height, previewWidth, previewHeight);
        mRotateFBO.updateSurfaceSize(width, height);
    }

    public void onSurfaceDestroyed() {
        mTrackerManager.onSurfaceDestroyed();
        mRotateFBO.release();
    }

    public void switchCameraShortVideo(int ordinal) {
        mCameraId = ordinal % 2;
        mTrackerManager.switchCamera(mCameraId);
        setConfigShortVideo(mCameraId);
    }

    /**?
     * 由于屏幕Accelerometer获取的方向和kwsdk中tracker的方向是同一意思，所以需要重新计算
     * @return 返回人脸朝向
     */
    public int computeShortVideo() {
        int dir = Accelerometer.getDirection();

        //dir = dir + 2;
        dir = dir + 2;
        dir = dir % 4;

        if(mCameraId == 1) {
            switch (dir) {
                case 0:
                    dir = 1;
                    break;
                case 1:
                    dir = 0;
                    break;
                case 2:
                    dir = 3;
                    break;
                case 3:
                    dir = 2;
                    break;
            }
        } else {
            switch (dir) {
                case 0:
                    dir = 1;
                    break;
                case 1:
                    dir = 0;
                    break;
                case 2:
                    dir = 3;
                    break;
                case 3:
                    dir = 2;
                    break;
            }
        }
        return dir;
    }

    /**
     *
     * @param texId 纹理ID，此处的问题是内部TEXTURE_2D纹理
     * @param texWidth 宽高，处理后纹理的宽高
     * @param texHeight 宽高，处理后纹理的宽高
     * @param dir 方向，用以表示旋转角度，值为0-3，0表示0度，1表示90，2表示180，3表示270
     * @return 返回的是处理后的纹理ID
     * */
    public int onDrawTexture(int texId, int texWidth, int texHeight, int dir) {
        return mRotateFBO.draw(texId, texWidth,texHeight,dir);
    }


    public int drawTexture2D(int textureId, int texWidth, int texHeight){
        int faceDir = computeShortVideo();
        TextureUtils.setFaceDir(faceDir);
        boolean rotate = TextureUtils.getXYRotate();
        int dir = TextureUtils.getDir();
        int rotateID = textureId;
        if(dir == TextureUtils.DIR_270) {
            mFirstDir = 1;
            mLastDir = 3;
        } else if(dir == TextureUtils.DIR_90) {
            mFirstDir = 3;
            mLastDir = 1;
        }
        if(rotate && dir != TextureUtils.DIR_0) {
            rotateID = onDrawTexture(textureId,texWidth,texHeight,mFirstDir);
        }
        int sdkID = mTrackerManager.onDrawTexture2D(rotateID,texWidth,texHeight,1);

        if(rotate && dir != TextureUtils.DIR_0) {
            sdkID = onDrawTexture(sdkID,texWidth,texHeight,mLastDir);
        }
        return sdkID;
    }

    /**
     * UI事件处理类
     *
     * @param uiClickListener
     * @return
     */
    public OnViewEventListener initUIEventListener(final UIClickListener uiClickListener) {
        OnViewEventListener eventListener = new OnViewEventListener() {

            @Override
            public void onSwitchBeauty2(boolean enable) {
                mTrackerManager.setBeauty2Enabled(enable);
            }

            @Override
            public void onTakeShutter() {
                if(uiClickListener != null)
                uiClickListener.onTakeShutter();
            }

            @Override
            public void onSwitchCamera() {
                if(uiClickListener != null)
                uiClickListener.onSwitchCamera();
            }

            @Override
            public void onSwitchFilter(KwFilter filter) {
                mTrackerManager.switchFilter(filter);
            }

            @Override
            public void onStickerChanged(StickerConfig item) {
                mTrackerManager.switchSticker(item);
            }

            @Override
            public void onSwitchBeauty(boolean enable) {
                mTrackerManager.setBeautyEnabled(enable);
            }

            @Override
            public void onSwitchBeautyFace(boolean enable) {
                mTrackerManager.setBeautyFaceEnabled(enable);
            }

            @Override
            public void onDistortionChanged(KwFilterType filterType) {
                mTrackerManager.switchDistortion(filterType);
            }

            @Override
            public void onAdjustFaceBeauty(int type, float param) {
                switch (type) {
                    case BEAUTY_BIG_EYE_TYPE:
                        mTrackerManager.setEyeMagnifying((int) param);
                        break;
                    case BEAUTY_THIN_FACE_TYPE:
                        mTrackerManager.setChinSliming((int) param);
                        break;
                    case SKIN_SHINNING_TENDERNESS:
                        //粉嫩
                        mTrackerManager.setSkinTenderness((int) param);
                        break;
                    case SKIN_TONE_SATURATION:
                        //饱和
                        mTrackerManager.setSkinSaturation((int) param);
                        break;
                    case REMOVE_BLEMISHES:
                        //磨皮
                        mTrackerManager.setSkinBlemishRemoval((int) param);
                        break;
                    case SKIN_TONE_PERFECTION:
                        //美白
                        mTrackerManager.setSkinWhitening((int) param);
                        break;
                }

            }

            @Override
            public void onFaceBeautyLevel(float level) {
                mTrackerManager.adjustBeauty(level);
            }

        };

        return eventListener;
    }
}
