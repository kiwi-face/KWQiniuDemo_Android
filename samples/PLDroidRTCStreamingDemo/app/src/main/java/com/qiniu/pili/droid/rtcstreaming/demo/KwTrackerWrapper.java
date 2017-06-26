package com.qiniu.pili.droid.rtcstreaming.demo;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.KwFilter;
import com.kiwi.tracker.bean.KwRenderResult;
import com.kiwi.tracker.bean.KwYuvFrame;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.tracker.fbo.RgbaToNv21FBO;
import com.kiwi.tracker.utils.GlUtil;
import com.kiwi.ui.OnViewEventListener;
import com.kiwi.ui.helper.ResourceHelper;
import com.kiwi.ui.model.SharePreferenceMgr;

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
    public interface UIClickListener {
        void onTakeShutter();

        void onSwitchCamera();
    }

    private KwTrackerSettings mTrackerSetting;
    private KwTrackerManager mTrackerManager;

    public KwTrackerWrapper(Context context, int cameraFaceId) {

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
                setTrackerSetting(mTrackerSetting).
                build();

        //copy assets config/sticker/filter to sdcard
        ResourceHelper.copyResource2SD(context);
        //关闭日志打印
        Config.isDebug = false;
    }

    public void onCreate(Activity activity) {
        Log.i("tracker", "onCreate");

        mTrackerManager.onCreate(activity);
    }

    public void onResume(Activity activity) {
        Log.i("tracker", "onResume");
        mTrackerManager.onResume(activity);

        if (rgbaToNv21FBO != null) {
            rgbaToNv21FBO.release();
            rgbaToNv21FBO = null;
        }

    }

    public void onPause(Activity activity) {
        Log.i("tracker", "onPause");
        mTrackerManager.onPause(activity);
    }

    public void onDestroy(Activity activity) {
        Log.i("tracker", "onDestroy");
        mTrackerManager.onDestory(activity);
    }

    public OnViewEventListener initUIEventListener(final Runnable switchCamearRunnable) {
        OnViewEventListener eventListener = new OnViewEventListener() {

            @Override
            public void onSwitchBeauty2(boolean enable) {
                mTrackerManager.setBeauty2Enabled(enable);
            }

            @Override
            public void onTakeShutter() {

            }

            @Override
            public void onSwitchCamera() {
                switchCamearRunnable.run();
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

            /**
             * set face beauty param
             * @param type type
             * @param param [0~100]
             */
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
                if (uiClickListener != null)
                    uiClickListener.onTakeShutter();
            }

            @Override
            public void onSwitchCamera() {
                if (uiClickListener != null)
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

            /**
             * set face beauty param
             * @param type type
             * @param param [0~100]
             */
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

    public boolean isNeedTrack() {
        return mTrackerSetting.isNeedTrack();
    }

    public KwRenderResult renderYuvFrame(KwYuvFrame kwYuvFrame) {
        return mTrackerManager.renderYuvFrame(kwYuvFrame);
    }

    public void onSurfaceCreated(Context context) {
        mTrackerManager.onSurfaceCreated(context);
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        mTrackerManager.onSurfaceChanged(width, height, previewWidth, previewHeight);
    }

    public void onSurfaceDestroyed() {
        mTrackerManager.onSurfaceDestroyed();
    }

    /**
     * @param texId     YUV格式纹理
     *                  对纹理进行特效处理（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrame(int texId, int texWidth, int texHeight) {
        long start = System.currentTimeMillis();

        //解开绑定
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        int newTexId = texId;
//        int maxFaceCount = 1;
        int filterTexId = mTrackerManager.onDrawOESTexture(texId, texWidth, texHeight, 1);
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
