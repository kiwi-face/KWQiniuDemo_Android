package com.qiniu.pili.droid.rtcstreaming.demo.activity.conference;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioInfo;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioLevelCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceManager;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCFrameCapturedCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaSubscribeCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteAudioEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCStreamStats;
import com.qiniu.pili.droid.rtcstreaming.RTCUserEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.StreamUtils;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.widget.AspectFrameLayout;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *  演示使用 SDK 互动专版 API，不带推流，但有更加丰富的接口和回调
 */
public class ConferenceActivity extends AppCompatActivity {

    private static final String TAG = "ConferenceActivity";

    private Button mControlButton;
    private CheckBox mMuteCheckBox;

    private FloatingActionButton mMuteSpeakerButton;
    private FloatingActionButton mVideoCaptureButton;
    private FloatingActionButton mAudioCaptureButton;
    private FloatingActionButton mVideoPublishButton;
    private FloatingActionButton mAudioPublishButton;

    private ProgressDialog mProgressDialog;

    private RTCConferenceManager mRTCConferenceManager;

    private int mCurrentCamFacingIndex;

    private GLSurfaceView mCameraPreviewFrameView;
    private RTCVideoWindow mRTCVideoWindowA;
    private RTCVideoWindow mRTCVideoWindowB;

    private String mRoomName;

    private boolean mIsSpeakerMuted = false;
    private boolean mIsStatsEnabled = false;

    private boolean mIsVideoCaptureStarted = false;
    private boolean mIsAudioCaptureStarted = false;
    private boolean mIsVideoPublishStarted = false;
    private boolean mIsAudioPublishStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_conference);

        /**
         * Step 1: init sdk, you can also move this to Application.onCreate
         */
        RTCMediaStreamingManager.init(getApplicationContext());

        /**
         * Step 2: find & init views
         */
        AspectFrameLayout afl = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
        afl.setShowMode(AspectFrameLayout.SHOW_MODE.FULL);
        mCameraPreviewFrameView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);

        mRoomName = getIntent().getStringExtra("roomName");

        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        boolean isLandscape = getIntent().getBooleanExtra("orientation", false);
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        boolean isBeautyEnabled = getIntent().getBooleanExtra("beauty", false);
        boolean isDebugModeEnabled = getIntent().getBooleanExtra("debugMode", false);

        boolean audioLevelCallback = getIntent().getBooleanExtra("audioLevelCallback", false);
        mIsStatsEnabled = getIntent().getBooleanExtra("enableStats", false);

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);

        mMuteSpeakerButton = (FloatingActionButton) findViewById(R.id.MuteSpeakerBtn);
        mVideoCaptureButton = (FloatingActionButton) findViewById(R.id.VideoCaptureBtn);
        mAudioCaptureButton = (FloatingActionButton) findViewById(R.id.AudioCaptureBtn);
        mVideoPublishButton = (FloatingActionButton) findViewById(R.id.VideoPublishBtn);
        mAudioPublishButton = (FloatingActionButton) findViewById(R.id.AudioPublishBtn);

        /**
         * Step 3: config camera settings
         */
        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

        if (isBeautyEnabled) {
            cameraStreamingSetting.setBuiltInFaceBeautyEnabled(true); // Using sdk built in face beauty algorithm
            cameraStreamingSetting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)); // sdk built in face beauty settings
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off
        }

        /**
         * Step 4: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mRTCConferenceManager = new RTCConferenceManager(getApplicationContext(), afl, mCameraPreviewFrameView, codecType);
        mRTCConferenceManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCConferenceManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCConferenceManager.setUserEventListener(mRTCUserEventListener);
        mRTCConferenceManager.setDebugLoggingEnabled(isDebugModeEnabled);
        mRTCConferenceManager.setMediaSubscribeCallback(mRTCMediaSubscribeCallback);
        mRTCConferenceManager.setRemoteAudioEventListener(mRTCRemoteAudioEventListener);

        /**
         * The audio level callback will cause 5% cpu occupation
         */
        if (audioLevelCallback) {
            mRTCConferenceManager.setAudioLevelCallback(mRTCAudioLevelCallback);
        }

        /**
         * Step 5: set conference options
         */
        RTCConferenceOptions options = new RTCConferenceOptions();
        options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_16_9);
        options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_480);
        options.setVideoBitrateRange(800 * 1024, 1024 * 1024);
        options.setVideoEncodingFps(15);
        options.setHWCodecEnabled(!isSwCodec);
        mRTCConferenceManager.setConferenceOptions(options);

        /**
         * Step 6: create the remote windows, must add enough windows for remote users
         */
        RTCVideoWindow windowA = new RTCVideoWindow(findViewById(R.id.RemoteWindowA), (GLSurfaceView)findViewById(R.id.RemoteGLSurfaceViewA));
        RTCVideoWindow windowB = new RTCVideoWindow(findViewById(R.id.RemoteWindowB), (GLSurfaceView)findViewById(R.id.RemoteGLSurfaceViewB));

        /**
         * Step 7: add the remote windows
         */
        mRTCConferenceManager.addRemoteWindow(windowA);
        mRTCConferenceManager.addRemoteWindow(windowB);

        mRTCVideoWindowA = windowA;
        mRTCVideoWindowB = windowB;

        /**
         * Step 8: do prepare
         */
        mRTCConferenceManager.prepare(cameraStreamingSetting, null);

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRTCConferenceManager.startVideoCapture();
        mIsVideoCaptureStarted = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRTCConferenceManager.stopVideoCapture();
        mIsVideoCaptureStarted = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 9: You must call destroy to release some resources when activity destroyed
         */
        mRTCConferenceManager.destroy();
        /**
         * Step 10: You can also move this to your MainActivity.onDestroy
         */
        RTCMediaStreamingManager.deinit();
    }

    public void onClickExit(View v) {
        finish();
    }

    private boolean startConference() {
        if (mRTCConferenceManager.isConferenceStarted()) {
            return true;
        }
        mProgressDialog.setMessage("正在加入连麦 ... ");
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startConferenceInternal();
            }
        }).start();
        return true;
    }

    private boolean startConferenceInternal() {
        String roomToken = StreamUtils.requestRoomToken(StreamUtils.getTestUserId(this), mRoomName);
        if (roomToken == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息 !", Toast.LENGTH_SHORT);
            return false;
        }
        mRTCConferenceManager.startConference(StreamUtils.getTestUserId(this), mRoomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                dismissProgressDialog();
                showToast(getString(R.string.start_conference), Toast.LENGTH_SHORT);
                updateControlButtonText();
                /**
                 * will cost 2% more cpu usage if enabled
                 */
                mRTCConferenceManager.setStreamStatsEnabled(mIsStatsEnabled);
            }

            @Override
            public void onStartConferenceFailed(int errorCode) {
                dismissProgressDialog();
                showToast(getString(R.string.failed_to_start_conference) + errorCode, Toast.LENGTH_SHORT);
            }
        });
        return true;
    }

    private boolean stopConference() {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            return true;
        }
        mRTCConferenceManager.stopConference();
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    public void onClickRemoteWindowA(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mCameraPreviewFrameView.getId()) {
            mRTCConferenceManager.switchRenderView(mCameraPreviewFrameView, mRTCVideoWindowA.getGLSurfaceView());
        } else {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowA.getGLSurfaceView(), mCameraPreviewFrameView);
        }
    }

    public void onClickRemoteWindowB(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mCameraPreviewFrameView.getId()) {
            mRTCConferenceManager.switchRenderView(mCameraPreviewFrameView, mRTCVideoWindowB.getGLSurfaceView());
        } else {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowB.getGLSurfaceView(), mCameraPreviewFrameView);
        }
    }

    public void onClickConference(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            startConference();
        } else {
            stopConference();
        }
    }

    public void onClickKickoutUserA(View v) {
        mRTCConferenceManager.kickoutUser(R.id.RemoteGLSurfaceViewA);
    }

    public void onClickKickoutUserB(View v) {
        mRTCConferenceManager.kickoutUser(R.id.RemoteGLSurfaceViewB);
    }

    public void onClickSwitchCamera(View v) {
        mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
        CameraStreamingSetting.CAMERA_FACING_ID facingId;
        if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        }
        Log.i(TAG, "switchCamera:" + facingId);
        mRTCConferenceManager.switchCamera(facingId);
    }

    public void onClickCaptureFrame(View v) {
        mRTCConferenceManager.captureFrame(new RTCFrameCapturedCallback() {
            @Override
            public void onFrameCaptureSuccess(Bitmap bitmap) {
                String filepath = Environment.getExternalStorageDirectory() + "/captured.jpg";
                saveBitmapToSDCard(filepath, bitmap);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filepath)));
                showToast("截帧成功, 存放在 " + filepath, Toast.LENGTH_SHORT);
            }

            @Override
            public void onFrameCaptureFailed(int errorCode) {
                showToast("截帧失败，错误码：" + errorCode, Toast.LENGTH_SHORT);
            }
        });
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRTCConferenceManager.isConferenceStarted()) {
                    mControlButton.setText(getString(R.string.stop_conference));
                } else {
                    mControlButton.setText(getString(R.string.start_conference));
                }
            }
        });
    }

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRTCConferenceManager.mute(mMuteCheckBox.isChecked());
        }
    };

    public void onClickVideoCapture(View v) {
        if (mIsVideoCaptureStarted) {
            mRTCConferenceManager.stopVideoCapture();
            mIsVideoCaptureStarted = false;
            mVideoCaptureButton.setTitle("采集视频");
        } else {
            mRTCConferenceManager.startVideoCapture();
            mIsVideoCaptureStarted = true;
            mVideoCaptureButton.setTitle("停止视频采集");
        }
    }

    public void onClickAudioCapture(View v) {
        if (mIsAudioCaptureStarted) {
            mRTCConferenceManager.stopAudioCapture();
            mIsAudioCaptureStarted = false;
            mAudioCaptureButton.setTitle("采集音频");
        } else {
            mRTCConferenceManager.startAudioCapture();
            mIsAudioCaptureStarted = true;
            mAudioCaptureButton.setTitle("停止音频采集");
        }
    }

    public void onClickMuteSpeaker(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsSpeakerMuted) {
            mRTCConferenceManager.muteSpeaker(false);
            mMuteSpeakerButton.setTitle(getResources().getString(R.string.button_mute_speaker));
            mIsSpeakerMuted = false;
        } else {
            mRTCConferenceManager.muteSpeaker(true);
            mMuteSpeakerButton.setTitle(getResources().getString(R.string.button_unmute_speaker));
            mIsSpeakerMuted = true;
        }
    }

    public void onClickVideoPublish(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsVideoPublishStarted) {
            mRTCConferenceManager.unpublishLocalVideo();
            mIsVideoPublishStarted = false;
            mVideoPublishButton.setTitle("发布视频");
        } else{
            mRTCConferenceManager.publishLocalVideo();
            mIsVideoPublishStarted = true;
            mVideoPublishButton.setTitle("取消视频发布");
        }
    }

    public void onClickAudioPublish(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsAudioPublishStarted) {
            mRTCConferenceManager.unpublishLocalAudio();
            mIsAudioPublishStarted = false;
            mAudioPublishButton.setTitle("发布音频");
        } else{
            mRTCConferenceManager.publishLocalAudio();
            mIsAudioPublishStarted = true;
            mAudioPublishButton.setTitle("取消音频发布");
        }
    }

    public void onClickPrintStats(View v) {
        HashMap<String, RTCStreamStats> stats = mRTCConferenceManager.getStreamStats();
        if (stats == null || stats.isEmpty()) {
            showToast(getString(R.string.log_stats_failed), Toast.LENGTH_SHORT);
            return;
        }
        for (Map.Entry<String, RTCStreamStats> entry : stats.entrySet()) {
            Log.i(TAG, "用户: " + entry.getKey()
                    + " 帧率:" + entry.getValue().getFrameRate()
                    + " 上行:" + entry.getValue().getSentBitrate() + "bps"
                    + " 下行:" + entry.getValue().getRecvBitrate() + "bps"
                    + " 丢包率:" + entry.getValue().getPacketLossPercent() + "%");
        }
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
                    showToast(getString(R.string.ready), Toast.LENGTH_SHORT);
                    break;
                case CONNECT_FAIL:
                    showToast(getString(R.string.failed_to_connect_rtc_server), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_FAILED:
                case AUDIO_PUBLISH_FAILED:
                    showToast(getString(R.string.failed_to_publish_av_to_rtc) + extra, Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_video_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_audio_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case USER_JOINED_AGAIN:
                    showToast(getString(R.string.user_join_other_where), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case USER_KICKOUT_BY_HOST:
                    showToast(getString(R.string.user_kickout_by_host), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case OPEN_CAMERA_FAIL:
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                default:
                    break;
            }
        }
    };

    private RTCUserEventListener mRTCUserEventListener = new RTCUserEventListener() {
        @Override
        public void onUserJoinConference(String remoteUserId) {
            Log.i(TAG, "onUserJoinConference: " + remoteUserId);
        }

        @Override
        public void onUserLeaveConference(String remoteUserId) {
            Log.i(TAG, "onUserLeaveConference: " + remoteUserId);
        }
    };

    private RTCRemoteWindowEventListener mRTCRemoteWindowEventListener = new RTCRemoteWindowEventListener() {
        @Override
        public void onRemoteWindowAttached(RTCVideoWindow window, String remoteUserId) {
            Log.i(TAG, "onRemoteWindowAttached: " + remoteUserId);
        }

        @Override
        public void onRemoteWindowDetached(RTCVideoWindow window, String remoteUserId) {
            Log.i(TAG, "onRemoteWindowDetached: " + remoteUserId);
        }

        @Override
        public void onFirstRemoteFrameArrived(String s) {

        }
    };

    private RTCMediaSubscribeCallback mRTCMediaSubscribeCallback = new RTCMediaSubscribeCallback() {
        @Override
        public boolean isSubscribeVideoStream(String fromUserId) {
            Log.i(TAG, "remote video published: " + fromUserId);
            /**
             * decided whether subscribe the video stream
             * return true -- do subscribe, return false will ignore the video stream
             */
            return true;
        }
    };

    private RTCRemoteAudioEventListener mRTCRemoteAudioEventListener = new RTCRemoteAudioEventListener() {
        @Override
        public void onRemoteAudioPublished(String userId) {
            Log.i(TAG, "onRemoteAudioPublished userId = " + userId);
        }

        @Override
        public void onRemoteAudioUnpublished(String userId) {
            Log.i(TAG, "onRemoteAudioUnpublished userId = " + userId);
        }
    };

    private RTCAudioLevelCallback mRTCAudioLevelCallback = new RTCAudioLevelCallback() {
        @Override
        public void onAudioLevelChanged(RTCAudioInfo rtcAudioInfo) {
            Log.i(TAG, "onAudioLevelChanged: " + rtcAudioInfo.toString());
        }
    };

    private CameraStreamingSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConferenceActivity.this, text, duration).show();
            }
        });
    }

    private static boolean saveBitmapToSDCard(String filepath, Bitmap bitmap) {
        try {
            FileOutputStream fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
