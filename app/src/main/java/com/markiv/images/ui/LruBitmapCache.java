package com.markiv.images.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.android.volley.toolbox.ImageLoader.ImageCache;

//TODO Add disk cache
public class LruBitmapCache extends LruCache<String, Bitmap>
        implements ImageCache {

    private static LruBitmapCache sBitmapCache;
    private LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }

    public static LruBitmapCache getInstance(Context context) {
        if(sBitmapCache == null) {
            final DisplayMetrics displayMetrics = context.getResources().
                    getDisplayMetrics();
            final int screenWidth = displayMetrics.widthPixels;
            final int screenHeight = displayMetrics.heightPixels;

            // 4 bytes per pixel
            final int screenBytes = screenWidth * screenHeight * 4;
            final int size = screenBytes * 3;
            sBitmapCache = new LruBitmapCache(size);
        }

        return sBitmapCache;
    }
}
