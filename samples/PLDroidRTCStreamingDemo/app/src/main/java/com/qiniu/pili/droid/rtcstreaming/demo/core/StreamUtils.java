package com.qiniu.pili.droid.rtcstreaming.demo.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class StreamUtils {

    public static final boolean IS_USING_STREAMING_JSON = false;

    // 区分主播和副主播
    public static final int RTC_ROLE_ANCHOR = 0x01;
    public static final int RTC_ROLE_VICE_ANCHOR = 0x02;

    // 用户 ID 可以使用业务上的用户 ID
    private static String mUserId;

    // 业务服务器的地址，需要能提供推流地址、播放地址、RoomToken
    // 这里原本填写的是七牛的测试业务服务器，现在需要改写为客户自己的业务服务器
    // 例如：http://www.xxx.com/api/
    public static String getAppServerAddr() {
        return "";
    }

    // 为了 Demo 的演示方便，建议服务器提供一个获取固定推流地址的链接
    // 传给服务器一个 “房间号” ，由服务器根据 “房间号” 返回一个固定的推流地址
    // 例如：http://www.xxx.com/api/stream/room001
    // 这个 “房间号” 必须是业务服务器事先手动为 “主播” 创建的 “连麦房间号”，不能随意设置
    public static String requestPublishAddress(String roomName) {
        String url = getAppServerAddr() + "/stream/" + roomName;
        return doRequest("POST", url);
    }

    // 为了 Demo 的演示方便，建议服务器提供一个获取固定播放地址的链接
    // 传给服务器一个 “房间号” ，由服务器根据 “房间号” 返回一个固定的播放地址，跟上面推流地址“匹配”
    // 例如：http://www.xxx.com/api/play/room001
    // 这个 “房间号” 必须是业务服务器事先手动为 “主播” 创建的 “连麦房间号”，不能随意设置
    public static String requestPlayURL(String roomName) {
        String url = getAppServerAddr() + "/stream/" + roomName + "/play";
        return doRequest("GET", url);
    }

    // 为了 Demo 的演示方便，建议服务器提供一个获取 "RoomToken" 的链接
    // 传给服务器 "用户名" 和 “房间号” ，由服务器根据 "用户名" “房间号” 生成一个 roomToken
    // 客户端再以这个 "用户名"、“房间号”、“roomToken” 去加入房间
    // 例如：http://www.xxx.com/api/room/room001/user/user001/token
    // 这个 “房间号” 必须是业务服务器事先手动为 “主播” 创建的 “连麦房间号”，不能随意设置
    // 可以用我们的工具来生成相关测试房间和地址：https://github.com/qiniudemo/qrtc
    public static String requestRoomToken(String userId, String roomName) {
        String url = getAppServerAddr() + "/room/" + roomName + "/user/" + userId + "/token";
        return doRequest("POST", url);
    }

    // 发送 HTTP 请求获取相关的地址信息
    private static String doRequest(String method, String url) {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            httpConn.setRequestMethod(method);
            httpConn.setConnectTimeout(5000);
            httpConn.setReadTimeout(10000);
            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int length = httpConn.getContentLength();
            if (length <= 0) {
                return null;
            }
            InputStream is = httpConn.getInputStream();
            byte[] data = new byte[length];
            int read = is.read(data);
            is.close();
            if (read <= 0) {
                return null;
            }
            return new String(data, 0, read);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static String getTestUserId(Context context) {
        if (mUserId != null) {
            return mUserId;
        }
        SharedPreferences preferences = context.getSharedPreferences("rtc", Context.MODE_PRIVATE);
        if (!preferences.contains("user_id")) {
            mUserId = "qiniu-" + UUID.randomUUID().toString();
            preferences.edit().putString("user_id", mUserId).apply();
        } else {
            mUserId = preferences.getString("user_id", "");
        }
        return mUserId;
    }
}
