package com.markiv.images.ui.image;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.markiv.images.data.VolleyProvider;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class ImageLoader2 {
    private static ImageLoader2 sImageLoader2;

    private RequestQueue mRequestQueue;

    public static synchronized ImageLoader2 getInstance(Context context){
        if(sImageLoader2 == null){
            sImageLoader2 = new ImageLoader2(context);
        }
        return sImageLoader2;
    }

    private ImageLoader2(Context context) {
        mRequestQueue = VolleyProvider.getInstance(context).getImageRequestQueue();
    }

    public void loadImage(String url, Listener listener){

    }

    public interface Listener {
        public void onDisplayLoadingImage();
        public void onImageLoaded();
        public void onImageLoadFailed();
    }
}
