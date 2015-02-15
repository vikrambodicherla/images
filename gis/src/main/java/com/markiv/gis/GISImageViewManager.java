package com.markiv.gis;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.markiv.gis.image.GISImageView;
import com.markiv.gis.image.LruBitmapCache;

/**
 * @author vikrambd
 * @since 2/14/15
 */
public class GISImageViewManager {
    private final Context mContext;
    private final RequestQueue mRequestQueue;
    private final LruBitmapCache mBitmapCache;

    private final ImageLoader mImageLoader;

    public GISImageViewManager(Context context, RequestQueue requestQueue, LruBitmapCache bitmapCache) {
        mContext = context;

        mRequestQueue = requestQueue;
        mBitmapCache = bitmapCache;

        mImageLoader = new ImageLoader(mRequestQueue, mBitmapCache);
    }

    public GISImageView newImageView(){
        return new GISImageView(mContext, mImageLoader);
    }

    public void cancelAll(){
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public void stop(){
        mBitmapCache.evictAll();
        mRequestQueue.stop();
    }
}
