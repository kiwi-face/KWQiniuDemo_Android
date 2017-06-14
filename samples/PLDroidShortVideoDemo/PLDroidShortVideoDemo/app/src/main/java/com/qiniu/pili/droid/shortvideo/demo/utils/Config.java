package com.qiniu.pili.droid.shortvideo.demo.utils;

import android.os.Environment;

import com.qiniu.android.common.Zone;

public class Config {
    public static final String TOKEN = "MqF35-H32j1PH8igh-am7aEkduP511g-5-F7j47Z:0gzBOkhm3KsFGbGk2HdKfA4jZp4=:eyJzY29wZSI6InNob3J0LXZpZGVvIiwiZGVhZGxpbmUiOjE2NTA3MTExMDcsInVwaG9zdHMiOlsiaHR0cDovL3VwLXoyLnFpbml1LmNvbSIsImh0dHA6Ly91cGxvYWQtejIucWluaXUuY29tIiwiLUggdXAtejIucWluaXUuY29tIGh0dHA6Ly8xODMuNjAuMjE0LjE5OCJdfQ==";
    public static final String ak = "MqF35-H32j1PH8igh-am7aEkduP511g-5-F7j47Z";
    public static Zone DEFAULT_ZONE = Zone.httpsAutoZone;
    public static final String DOMAIN = "oowtvx1xy.bkt.clouddn.com";

    public static final String VIDEO_STORAGE_DIR = Environment.getExternalStorageDirectory() + "/ShortVideo";
    public static final String RECORD_FILE_PATH = VIDEO_STORAGE_DIR + "/record.mp4";
    public static final String EDITED_FILE_PATH = VIDEO_STORAGE_DIR + "/edited.mp4";
}
