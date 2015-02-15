package com.markiv.images;

import android.app.Application;
import android.os.StrictMode;

/**
 * @author vikrambd
 * @since 1/20/15
 */
public class GImageSearchApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Enable strictmode for debug
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
    }
}
