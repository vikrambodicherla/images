package com.markiv.gis.image;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.markiv.gis.network.VolleyProvider;

/**
* @author vikrambd
* @since 3/1/15
*/
public class ImageViewManager {
    private final Context mContext;

    private final RequestQueue mImageRequestQueue;
    private final ImageLoader mImageLoader;
    private final LruBitmapCache mLruBitmapCache;

    public ImageViewManager(Context context, VolleyProvider volleyProvider){
        mContext = context;
        mImageRequestQueue = volleyProvider.newRequestQueue(context.getApplicationContext());
        mLruBitmapCache = LruBitmapCache.newInstance(context);
        mImageLoader = new ImageLoader(mImageRequestQueue, mLruBitmapCache);
    }

    public GISImageView newImageView() {
        return new GISImageView(mContext, mImageLoader);
    }
    
    public void cancelAll(){
        mImageRequestQueue.cancelAll(sALL_TRUE);
    }

    public void stop(){
        mImageRequestQueue.stop();
    }

    public void cleanUp() {
        mLruBitmapCache.evictAll();
    }

    private static final RequestQueue.RequestFilter sALL_TRUE = new RequestQueue.RequestFilter() {
        @Override
        public boolean apply(Request<?> request) {
            return true;
        }
    };
    
    public static ImageViewManager newInstance(Context context){
        return new ImageViewManager(context, VolleyProvider.getInstance());
    }
}
