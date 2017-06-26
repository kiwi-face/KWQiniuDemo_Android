package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.PLNetworkManager;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.demo.BuildConfig;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.StreamUtils;
import com.squareup.leakcanary.LeakCanary;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private EditText mRoomEditText;
    private RadioGroup mCaptureRadioGroup;
    private RadioGroup mCodecRadioGroup;
    private RadioGroup mRTCModeRadioGroup;
    private RadioGroup mBitrateControlRadioGroup;
    private ProgressDialog mProgressDialog;
    private CheckBox mCheckBoxBeauty;
    private CheckBox mCheckBoxWatermark;
    private CheckBox mCheckBoxDebugMode;
    private CheckBox mCheckBoxCustomSetting;
    private CheckBox mCheckBoxAudioLevel;
    private CheckBox mCheckboxEnableStats;

    private static final String[] DEFAULT_PLAYBACK_DOMAIN_ARRAY = {
            "pili-live-rtmp.pilitest.qiniucdn.com",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LeakCanary.install(getApplication());
        RTCMediaStreamingManager.init(getApplicationContext());

        mCaptureRadioGroup = (RadioGroup) findViewById(R.id.CaptureRadioGroup);
        mCodecRadioGroup = (RadioGroup) findViewById(R.id.CodecRadioGroup);
        mRTCModeRadioGroup = (RadioGroup) findViewById(R.id.RTCModeGroup);
        mBitrateControlRadioGroup = (RadioGroup) findViewById(R.id.bitrate_control_group);

        mCheckBoxBeauty = (CheckBox) findViewById(R.id.CheckboxBeauty);
        mCheckBoxWatermark = (CheckBox) findViewById(R.id.CheckboxWatermark);
        mCheckBoxDebugMode = (CheckBox) findViewById(R.id.CheckboxDebugMode);
        mCheckBoxCustomSetting = (CheckBox) findViewById(R.id.CheckboxCustomSetting);
        mCheckBoxAudioLevel = (CheckBox) findViewById(R.id.CheckboxAudioLevel);
        mCheckboxEnableStats = (CheckBox) findViewById(R.id.CheckboxEnableStats);

        mRoomEditText = (EditText) findViewById(R.id.RoomNameEditView);
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mRoomEditText.setText(preferences.getString("roomName", ""));

        TextView versionInfoTextView = (TextView) findViewById(R.id.VersionInfoTextView);
        String info = "版本号：" + getVersionDescription() + "，编译时间：" + getBuildTimeDescription();
        versionInfoTextView.setText(info);

        MultiDex.install(this);
        try {
            PLNetworkManager.getInstance().startDnsCacheService(this, DEFAULT_PLAYBACK_DOMAIN_ARRAY);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mProgressDialog = new ProgressDialog(this);

        // Check server configuration
//        if ("".equals(StreamUtils.getAppServerAddr())) {
//            Toast.makeText(this, getString(R.string.server_not_configure), Toast.LENGTH_LONG).show();
//            finish();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        editor.putString("roomName", mRoomEditText.getText().toString());
        editor.apply();

        PLNetworkManager.getInstance().stopDnsCacheService(this);
    }

    public void onClickAnchor(View v) {
        if (mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK) {
            jumpToPKActivity(PKAnchorActivity.class);
            return;
        }
        switch (mCaptureRadioGroup.getCheckedRadioButtonId()) {
            case R.id.RadioInnerCap:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_ANCHOR, CapStreamingActivity.class);
                break;
            case R.id.RadioExtCap:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_ANCHOR, ExtCapStreamingActivity.class);
                break;
            case R.id.RadioAudioOnly:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_ANCHOR, CapAudioStreamingActivity.class);
                break;
            default:
                break;
        }
    }

    public void onClickViceAnchor(View v) {
        if (mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK) {
            jumpToPKActivity(PKViceAnchorActivity.class);
            return;
        }
        switch (mCaptureRadioGroup.getCheckedRadioButtonId()) {
            case R.id.RadioInnerCap:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_VICE_ANCHOR, CapStreamingActivity.class);
                break;
            case R.id.RadioExtCap:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_VICE_ANCHOR, ExtCapStreamingActivity.class);
                break;
            case R.id.RadioAudioOnly:
                jumpToStreamingActivity(StreamUtils.RTC_ROLE_VICE_ANCHOR, CapAudioStreamingActivity.class);
                break;
            default:
                break;
        }
    }

    public void onClickAudience(View v) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        mProgressDialog.setMessage("正在获取播放地址..");
        mProgressDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String playURL = StreamUtils.requestPlayURL(roomName);
                if (playURL == null) {
                    dismissProgressDialog();
                    showToastTips("无法获取播放地址或者房间信息 !");
                    return;
                }
                dismissProgressDialog();
                Log.d(TAG,"Playback: " + playURL);
                Intent intent = new Intent(MainActivity.this, PlaybackActivity.class);
                intent.putExtra("videoPath", playURL);
                intent.putExtra("roomName", roomName);
                boolean isExtCapture = mCaptureRadioGroup.getCheckedRadioButtonId() == R.id.RadioExtCap;
                boolean isAudioOnly = mCaptureRadioGroup.getCheckedRadioButtonId() == R.id.RadioAudioOnly;
                intent.putExtra("extCapture", isExtCapture);
                intent.putExtra("audioOnly", isAudioOnly);
                intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
                intent.putExtra("orientation", mRTCModeRadioGroup.getCheckedRadioButtonId() != R.id.RadioPortrait);
                intent.putExtra("pkmode", mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK);
                startActivity(intent);
            }
        });
    }

    private void jumpToPKActivity(Class<?> cls) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("roomName", roomName);
        intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
        intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
        intent.putExtra("watermark", mCheckBoxWatermark.isChecked());
        intent.putExtra("debugMode", mCheckBoxDebugMode.isChecked());
        intent.putExtra("customSetting", mCheckBoxCustomSetting.isChecked());
        intent.putExtra("enableStats", mCheckboxEnableStats.isChecked());
        intent.putExtra("bitrateControl",
                mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_auto ? "auto"
                        : (mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_manual ? "manual" : "diable"));
        startActivity(intent);
    }

    private void jumpToStreamingActivity(int role, Class<?> cls) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("role", role);
        intent.putExtra("roomName", roomName.trim());
        intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
        intent.putExtra("orientation", mRTCModeRadioGroup.getCheckedRadioButtonId() != R.id.RadioPortrait);
        intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
        intent.putExtra("watermark", mCheckBoxWatermark.isChecked());
        intent.putExtra("debugMode", mCheckBoxDebugMode.isChecked());
        intent.putExtra("customSetting", mCheckBoxCustomSetting.isChecked());
        intent.putExtra("audioLevelCallback", mCheckBoxAudioLevel.isChecked());
        intent.putExtra("bitrateControl",
                mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_auto ? "auto"
                        : (mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_manual ? "manual" : "diable"));
        startActivity(intent);
    }

    private void showToastTips(final String tips) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, tips, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private String getVersionDescription() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "未知";
    }

    protected String getBuildTimeDescription() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(BuildConfig.BUILD_TIMESTAMP);
    }
}
