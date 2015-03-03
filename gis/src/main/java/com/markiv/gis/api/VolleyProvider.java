package com.markiv.gis.api;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import java.io.File;

/**
 * @author vikrambd
 * @since 3/1/15
 */
public class VolleyProvider {
    private static final String DEFAULT_CACHE_DIR = "volleyimages";
    private static final VolleyProvider sVOLLEY_PROVIDER;

    private VolleyProvider(){

    }

    /**
     * Volley normally builds a cache on the local file system. This method tries to build one on
     * the external file system before falling back to the internal system incase of an error
     *
     * @param context
     * @return
     */
    public RequestQueue newRequestQueue(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        }

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        HttpStack stack;
        if (Build.VERSION.SDK_INT >= 9) {
            stack = new HurlStack();
        } else {
            // Prior to Gingerbread, HttpUrlConnection was unreliable.
            // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
            stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        queue.start();

        return queue;
    }

    public static VolleyProvider getInstance(){
        return sVOLLEY_PROVIDER;
    }

    static {
        sVOLLEY_PROVIDER = new VolleyProvider();
    }

}
