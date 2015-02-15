package com.markiv.gis.api;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Singleton Volley provider for all image queues
 * @author vikrambd
 * @since 1/20/15
 */
public class VolleyProvider {
    private static VolleyProvider sVolleyProvider;

    private final RequestQueue queue;
    private final RequestQueue mImageRequestQueue;

    private VolleyProvider(Context context) {
        queue = Volley.newRequestQueue(context.getApplicationContext());
        mImageRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public RequestQueue getRequestQueue() {
        return queue;
    }

    public RequestQueue getImageRequestQueue(){
        return mImageRequestQueue;
    }

    public static VolleyProvider getInstance(Context context) {
        if (sVolleyProvider == null) {
            sVolleyProvider = new VolleyProvider(context);
        }
        return sVolleyProvider;
    }
}