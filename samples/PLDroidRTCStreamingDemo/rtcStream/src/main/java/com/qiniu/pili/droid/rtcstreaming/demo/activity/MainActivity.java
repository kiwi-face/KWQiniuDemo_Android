package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.qiniu.pili.droid.rtcstreaming.demo.BuildConfig;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.conference.ConferenceEntryActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.RTCStreamingEntryActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView versionInfoTextView = (TextView) findViewById(R.id.version_info_textview);
        String info = "版本号：" + getVersionDescription() + "，编译时间：" + getBuildTimeDescription();
        versionInfoTextView.setText(info);
    }

    public void onClickRTCStreaming(View v) {
        Intent intent = new Intent(MainActivity.this, RTCStreamingEntryActivity.class);
        startActivity(intent);
    }

    public void onClickRTCConference(View v) {
        Intent intent = new Intent(MainActivity.this, ConferenceEntryActivity.class);
        startActivity(intent);
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
