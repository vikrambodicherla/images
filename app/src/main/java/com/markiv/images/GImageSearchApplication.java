package com.markiv.images;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * @author vikrambd
 * @since 1/20/15
 */
public class GImageSearchApplication extends Application {
    public static final String ACTION_GSAPI_ERROR = "com.markiv.images.action.GSAPI_ERROR";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void notifyError(Context context, String errorMessage){
        GImageSearchApplication application = (GImageSearchApplication) (context.getApplicationContext());
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_GSAPI_ERROR));
    }
}
