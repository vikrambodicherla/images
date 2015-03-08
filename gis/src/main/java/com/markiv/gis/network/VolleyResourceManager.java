package com.markiv.gis.network;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.markiv.gis.image.LruBitmapCache;

/**
 * Singleton Volley provider for all image queues
 * @author vikrambd
 * @since 1/20/15
 */
public class VolleyResourceManager {
    //private static VolleyResourceManager sVolleyResourceManager;

    private final RequestQueue queue;
    private final RequestQueue mImageRequestQueue;
    private final ImageLoader mImageLoader;

    public VolleyResourceManager(Context context) {
        queue = Volley.newRequestQueue(context.getApplicationContext());

        mImageRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        mImageLoader = new ImageLoader(mImageRequestQueue, LruBitmapCache.newInstance(context));
    }

    public RequestQueue getRequestQueue() {
        return queue;
    }

    /*public RequestQueue getImageRequestQueue(){
        return mImageRequestQueue;
    }*/

    /*public static VolleyResourceManager newInstance(Context context) {
        if (sVolleyResourceManager == null) {
            sVolleyResourceManager = new VolleyResourceManager(context);
        }
        return sVolleyResourceManager;
    }*/
}