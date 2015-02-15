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
public class GISImageManager {
    private final ImageLoader mImageLoader;
    private final Context mContext;
    private final RequestQueue mRequestQueue;

    public GISImageManager(Context context, RequestQueue requestQueue, LruBitmapCache bitmapCache) {
        mContext = context;
        mRequestQueue = requestQueue;

        mImageLoader = new ImageLoader(mRequestQueue, bitmapCache);
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
        mRequestQueue.stop();
    }

    public GISImageView newImageView(){
        return new GISImageView(mContext, mImageLoader);
    }


}
