package com.markiv.gis.api;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Singleton Volley provider for all image queues
 * @author vikrambd
 * @since 1/20/15
 */
public class VolleyRequestQueueProvider {
    private static VolleyRequestQueueProvider sVolleyRequestQueueProvider;

    private final Context mContext;

    private RequestQueue queue;
    private RequestQueue mImageRequestQueue;

    private VolleyRequestQueueProvider(Context context) {
        mContext = context;
    }

    public synchronized RequestQueue getRequestQueue() {
        if(queue == null){
            queue = Volley.newRequestQueue(mContext.getApplicationContext());
        }

        return queue;
    }

    public synchronized RequestQueue getImageRequestQueue(){
        if(mImageRequestQueue == null){
            mImageRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }

        return mImageRequestQueue;
    }

    public static VolleyRequestQueueProvider getInstance(Context context) {
        if (sVolleyRequestQueueProvider == null) {
            sVolleyRequestQueueProvider = new VolleyRequestQueueProvider(context);
        }
        return sVolleyRequestQueueProvider;
    }
}