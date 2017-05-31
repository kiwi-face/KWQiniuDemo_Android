package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.kiwi.ui.KwControlView;
import com.kiwi.ui.model.StickerConfigMgr;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLConcatStateListener;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.tracker.QiniuLiveTrackerWrapper;
import com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.demo.view.SectionProgressBar;

public class VideoRecordActivity extends Activity implements PLRecordStateListener, PLConcatStateListener, PLVideoFilterListener {
    private static final String TAG = "VideoRecordActivity";

    private PLShortVideoRecorder mShortVideoRecorder;

    private SectionProgressBar mSectionProgressBar;
    private ProgressDialog mProgressDialog;
    private View mRecordBtn;
    private View mDeleteBtn;
    private View mConcatBtn;
    private View mSwitchCameraBtn;
    private View mSwitchFlashBtn;

    private boolean mFlashEnabled;
    private QiniuLiveTrackerWrapper qiniuLiveTrackerWrapper;
    private KwControlView kwControlView;
    private PLCameraSetting mCameraSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_record);

        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.record_progressbar);
        GLSurfaceView preview = (GLSurfaceView) findViewById(R.id.preview);
        mRecordBtn = findViewById(R.id.record);
        mDeleteBtn = findViewById(R.id.delete);
        mConcatBtn = findViewById(R.id.concat);
        mSwitchCameraBtn = findViewById(R.id.switch_camera);
        mSwitchFlashBtn = findViewById(R.id.switch_flash);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Processing...");
        mProgressDialog.setCancelable(false);

        mShortVideoRecorder = new PLShortVideoRecorder();
        mShortVideoRecorder.setRecordStateListener(this);

        int previewSizeRatio = getIntent().getIntExtra("PreviewSizeRatio", 0);
        int previewSizeLevel = getIntent().getIntExtra("PreviewSizeLevel", 0);
        int encodingSizeLevel = getIntent().getIntExtra("EncodingSizeLevel", 0);
        int encodingBitrateLevel = getIntent().getIntExtra("EncodingBitrateLevel", 0);

        mCameraSetting = new PLCameraSetting();
        mCameraSetting.setCameraId(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT);
        mCameraSetting.setCameraPreviewSizeRatio(getPreviewSizeRatio(previewSizeRatio));
        mCameraSetting.setCameraPreviewSizeLevel(getPreviewSizeLevel(previewSizeLevel));

        PLMicrophoneSetting microphoneSetting = new PLMicrophoneSetting();

        PLVideoEncodeSetting videoEncodeSetting = new PLVideoEncodeSetting();
        videoEncodeSetting.setEncodingSizeLevel(getEncodingSizeLevel(encodingSizeLevel));
        videoEncodeSetting.setEncodingBitrate(getEncodingBitrateLevel(encodingBitrateLevel));

        PLAudioEncodeSetting audioEncodeSetting = new PLAudioEncodeSetting();

        PLFaceBeautySetting faceBeautySetting = new PLFaceBeautySetting(1.0f, 0.5f, 0.5f);
        faceBeautySetting.setEnable(false);

        PLRecordSetting recordSetting = new PLRecordSetting();
        recordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);

        mShortVideoRecorder.prepare(preview, mCameraSetting, microphoneSetting,
                videoEncodeSetting, audioEncodeSetting, faceBeautySetting, recordSetting);

        mShortVideoRecorder.setVideoFilterListener(this);

        mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
        mSectionProgressBar.setTotalTime(RecordSettings.DEFAULT_MAX_RECORD_DURATION);

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mSwitchCameraBtn.setEnabled(false);
                    v.setActivated(true);
                    mShortVideoRecorder.beginSection();
                    mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
                } else if (action == MotionEvent.ACTION_UP) {
                    mSwitchCameraBtn.setEnabled(true);
                    v.setActivated(false);
                    mShortVideoRecorder.endSection();
                    mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
                }

                return false;
            }
        });
        onSectionCountChanged(0, 0);

        initKiwiSdk();
    }

    private void initKiwiSdk() {
        StickerConfigMgr.setSelectedStickerConfig(null);

        qiniuLiveTrackerWrapper = new QiniuLiveTrackerWrapper(this, mCameraSetting.getCameraId());
        qiniuLiveTrackerWrapper.onCreate(this);

        kwControlView = (KwControlView) findViewById(R.id.camera_control_view);
        kwControlView.setOnEventListener(qiniuLiveTrackerWrapper.initUIEventListener(new Runnable() {
            @Override
            public void run() {
                onClickSwitchCamera(null);
            }
        }));
    }

    @Override
    public void onSurfaceCreated() {
        qiniuLiveTrackerWrapper.onSurfaceCreated(this);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        qiniuLiveTrackerWrapper.onSurfaceChanged(width, height, 480,
                640);
    }

    @Override
    public void onSurfaceDestroy() {
        qiniuLiveTrackerWrapper.onSurfaceDestroyed();
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, long l) {
//        int ret = texId;
        int ret = qiniuLiveTrackerWrapper.onDrawFrame(texId, texWidth, texHeight);
        Log.e("tracker", "onDrawFrame,in:" + texId + ",out:" + ret + ",w:" + texWidth + ",h:" + texHeight);
        return ret;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordBtn.setEnabled(false);
        mShortVideoRecorder.resume();

        qiniuLiveTrackerWrapper.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShortVideoRecorder.pause();

        qiniuLiveTrackerWrapper.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mShortVideoRecorder.destroy();

        qiniuLiveTrackerWrapper.onDestroy(this);
    }

    public void onClickDelete(View v) {
        if (!mShortVideoRecorder.deleteLastSection()) {
            ToastUtils.s(this, "delete last section failed!");
        }
    }

    public void onClickConcat(View v) {
        mProgressDialog.show();
        mShortVideoRecorder.concatSections(this);
    }

    public void onClickSwitchCamera(View v) {
        mShortVideoRecorder.switchCamera();

        qiniuLiveTrackerWrapper.switchCamera(mCameraSetting.getCameraId());
    }

    public void onClickSwitchFlash(View v) {
        mFlashEnabled = !mFlashEnabled;
        mShortVideoRecorder.setFlashEnabled(mFlashEnabled);
        mSwitchFlashBtn.setActivated(mFlashEnabled);
    }

    @Override
    public void onReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwitchFlashBtn.setVisibility(mShortVideoRecorder.isFlashSupport() ? View.VISIBLE : View.GONE);
                mFlashEnabled = false;
                mSwitchFlashBtn.setActivated(mFlashEnabled);
                mRecordBtn.setEnabled(true);
                ToastUtils.s(VideoRecordActivity.this, "ready to record now");
            }
        });
    }

    @Override
    public void onDurationTooShort() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "video too short");
            }
        });
    }

    @Override
    public void onSectionIncreased(long incDuration, long totalDuration, int sectionCount) {
        onSectionCountChanged(sectionCount, totalDuration);
        mSectionProgressBar.addBreakPointTime(totalDuration);
    }

    @Override
    public void onSectionDecreased(long decDuration, long totalDuration, int sectionCount) {
        onSectionCountChanged(sectionCount, totalDuration);
        mSectionProgressBar.removeLastBreakPoint();
    }

    @Override
    public void onRecordCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "record completed !");
            }
        });
    }

    @Override
    public void onConcatProgressUpdate(int num, int sectionCount) {

    }

    @Override
    public void onConcatFailed(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
                ToastUtils.s(VideoRecordActivity.this, "concat sections failed: " + errorCode);
            }
        });
    }

    @Override
    public void onConcatSuccess(final String filePath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
                VideoEditActivity.start(VideoRecordActivity.this, filePath);
            }
        });
    }

    private void onSectionCountChanged(final int count, final long totalTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeleteBtn.setEnabled(count > 0);
                mConcatBtn.setEnabled(totalTime >= RecordSettings.DEFAULT_MIN_RECORD_DURATION);
            }
        });
    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_RATIO getPreviewSizeRatio(int position) {
        return RecordSettings.PREVIEW_SIZE_RATIO_ARRAY[position];
    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_LEVEL getPreviewSizeLevel(int position) {
        return RecordSettings.PREVIEW_SIZE_LEVEL_ARRAY[position];
    }

    private PLVideoEncodeSetting.VIDEO_ENCODING_SIZE_LEVEL getEncodingSizeLevel(int position) {
        return RecordSettings.ENCODING_SIZE_LEVEL_ARRAY[position];
    }

    private int getEncodingBitrateLevel(int position) {
        return RecordSettings.ENCODING_BITRATE_LEVEL_ARRAY[position];
    }
}
