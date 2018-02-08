package com.qiniu.pili.droid.rtcstreaming.demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.blankj.utilcode.utils.Utils;
import com.kiwi.filter.utils.TextureUtils;
import com.kiwi.tracker.KwFaceTracker;
import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.KwFilter;
import com.kiwi.tracker.bean.KwRenderResult;
import com.kiwi.tracker.bean.KwTrackResult;
import com.kiwi.tracker.bean.KwYuvFrame;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.tracker.fbo.RgbaToNv21FBO;
import com.kiwi.tracker.fbo.RotateFBO;
import com.kiwi.tracker.utils.Accelerometer;
import com.kiwi.tracker.utils.FTCameraUtils;
import com.kiwi.tracker.utils.GlUtil;
import com.kiwi.tracker.utils.TrackerConstant;
import com.kiwi.ui.OnViewEventListener;
import com.kiwi.ui.helper.ResourceHelper;
import com.kiwi.ui.model.SharePreferenceMgr;

import java.util.Timer;
import java.util.TimerTask;

import static com.blankj.utilcode.utils.ImageUtils.getBitmap;
import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;
import static com.kiwi.tracker.common.Config.isDebug;
import static com.kiwi.ui.KwControlView.BEAUTY_BIG_EYE_TYPE;
import static com.kiwi.ui.KwControlView.BEAUTY_THIN_FACE_TYPE;
import static com.kiwi.ui.KwControlView.REMOVE_BLEMISHES;
import static com.kiwi.ui.KwControlView.SKIN_SHINNING_TENDERNESS;
import static com.kiwi.ui.KwControlView.SKIN_TONE_PERFECTION;
import static com.kiwi.ui.KwControlView.SKIN_TONE_SATURATION;

/**
 * All calls to the Face Effects library are encapsulated in this class
 * Created by shijian on 2016/9/28.
 */
public class KwTrackerWrapper {
    RotateFBO mRotateFBO;
    int mFirstDir,mLastDir;
    private int mCameraId;
    private static final String TAG = KwTrackerWrapper.class.getName();

    public interface UIClickListener {
        void onTakeShutter();

        void onSwitchCamera();
    }

    private KwTrackerSettings mTrackerSetting;
    private KwTrackerManager mTrackerManager;

    public KwTrackerWrapper(final Context context, int cameraFaceId) {

        Utils.init(context);
        Config.init(context);
        mCameraId = cameraFaceId % 2;
        setConfig();

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
                setCameraFaceId(mCameraId);

        mTrackerManager = new KwTrackerManager(context).
                setTrackerSetting(mTrackerSetting)
                .build();

        //copy assets config/sticker/filter to sdcard
        ResourceHelper.copyResource2SD(context);
        initKiwiConfig();
    }

    private void setConfig(){
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
            TextureUtils.setInverted(true);
        }else {
            TextureUtils.setInverted(false);
        }

        int cameraDegree = FTCameraUtils.getOrientation(Config.getContext(), mCameraId);
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

        //切换完成后，恢复，需要延迟一段时间加载
//        if (isSwitchCamera) {
//            TimerTask timerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    mTrackerManager.setBeauty2Enabled(isBeautyOpen);
//                    mTrackerManager.setBeautyFaceEnabled(isBigEyeOpen);
//                    mTrackerManager.switchSticker(currentSticker);
//                    mTrackerManager.switchFilter(currentFilter);
//                }
//            };
//            new Timer().schedule(timerTask, 1000);
//        }
    }

    public void onSurfaceDestroyed() {
        mTrackerManager.onSurfaceDestroyed();
        mRotateFBO.release();

        if (rgbaToNv21FBO != null) {
            rgbaToNv21FBO.release();
            rgbaToNv21FBO = null;
        }
    }

    //记录当前的用户选择
    private KwFilter currentFilter;
    private StickerConfig currentSticker;
    private boolean isBigEyeOpen;
    private boolean isBeautyOpen;
    private boolean isSwitchCamera;
    private KwFilter filterNull = new KwFilter("nofilter", "nofilter", "inner");

    public void switchCamera(int ordinal) {
        mTrackerManager.switchCamera(ordinal);
        mCameraId = ordinal % 2;
        setConfig();

        if (rgbaToNv21FBO != null) {
            rgbaToNv21FBO.release();
            rgbaToNv21FBO = null;
        }

        //切换摄像头的时候记录
//        isBigEyeOpen = mTrackerSetting.isBeautyFaceEnabled();
//        isBeautyOpen = mTrackerSetting.isBeauty2Enabled();
//        currentFilter = mTrackerSetting.getFilter() == null ? filterNull : mTrackerSetting.getFilter();
//        currentSticker = mTrackerSetting.getStickerConfig() == null ? StickerConfig.NO_STICKER : mTrackerSetting.getStickerConfig();

        //重置
//        mTrackerManager.setBeauty2Enabled(false);
//        mTrackerManager.setBeautyFaceEnabled(false);
//        mTrackerManager.switchSticker(StickerConfig.NO_STICKER);
//        mTrackerManager.switchFilter(filterNull);
//        isSwitchCamera = true;
    }

    public int getCameraId() {
        return mCameraId;
    }

    /**?
     * 由于屏幕Accelerometer获取的方向和kwsdk中tracker的方向是同一意思，所以需要重新计算
     * @return 返回人脸朝向
     */
    public int computeFaceDir() {
        int dir = Accelerometer.getDirection();
        int cameraDegree = FTCameraUtils.getOrientation(Config.getContext(), mCameraId);
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
            switch (cameraDegree){
                case 0:
                    dir = dir + 2;
                    break;
                case 90:
                    dir = dir + 3;
                    break;
                case 180:

                    break;
                case 270:
                    dir = dir + 1;
                    break;

            }
        }else {
            switch (cameraDegree){
                case 0:

                    break;
                case 90:
                    dir = dir + 1;
                    break;
                case 180:
                    dir = dir + 2;
                    break;
                case 270:
                    dir = dir + 3;
                    break;

            }
        }

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
                    dir = 3;
                    break;
                case 1:
                    dir = 0;
                    break;
                case 2:
                    dir = 1;
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

    public int drawOESTexture(int oesTextureId, int texWidth, int texHeight){
        int faceDir = computeFaceDir();
        TextureUtils.setFaceDir(faceDir);
        int texId = mTrackerManager.toTexture2D(oesTextureId,texWidth,texHeight);
        boolean rotate = TextureUtils.getXYRotate();
        int dir = TextureUtils.getDir();
        int rotateID = texId;
        if(dir == TextureUtils.DIR_270) {
            mFirstDir = 1;
            mLastDir = 3;
        } else if(dir == TextureUtils.DIR_90) {
            mFirstDir = 3;
            mLastDir = 1;
        }
        if(rotate && dir != TextureUtils.DIR_0) {
            rotateID = onDrawTexture(texId,texWidth,texHeight,mFirstDir);
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
                uiClickListener.onTakeShutter();
            }

            @Override
            public void onSwitchCamera() {
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

    private RgbaToNv21FBO rgbaToNv21FBO;
    private int mFrameId = 0;

    /**
     * 纹理转换成yuv输出
     *
     * @param context   context
     * @param textureId 输入纹理id
     * @param w         预览宽度
     * @param h         预览高度
     * @param outs      yuv输出
     */
    public void textureToNv21(Context context, int textureId, int w, int h, byte[] outs) {
        if (rgbaToNv21FBO == null) {
            rgbaToNv21FBO = new RgbaToNv21FBO(GLES20.GL_TEXTURE_2D, w, h);
            GlUtil.checkGlError("new RgbaToNv21FBO");
            rgbaToNv21FBO.initialize(context);
            GlUtil.checkGlError("int fbo");
        }

        rgbaToNv21FBO.drawFrame(textureId, w, h);

        //pbo抛弃前两帧
        if (mFrameId++ < 3) {
            return;
        }
        byte[] bytes = rgbaToNv21FBO.getBytes();
        int size = outs.length > bytes.length ? bytes.length : outs.length;
        System.arraycopy(bytes, 0, outs, 0, size);
    }

}
