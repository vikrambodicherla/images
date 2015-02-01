package com.markiv.images.ui.image;

import android.content.Context;
import android.widget.ImageView;

import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class NetworkImageView2 extends ImageView {
    private String mUrl;
    private final ImageLoader2 mImageLoader2;

    public NetworkImageView2(Context context, ImageLoader2 imageLoader2) {
        super(context);
        mImageLoader2 = imageLoader2;
    }

    public void setImageUrl(String url, ImageLoader2 imageLoader2){
        mUrl = url;
        mImageLoader2.loadImage(url, new ImageLoader2.Listener() {
            @Override
            public void onDisplayLoadingImage() {
                setImageResource(R.drawable.ic_launcher);
            }

            @Override
            public void onImageLoadFailed() {

            }

            @Override
            public void onImageLoaded() {
                setImageResource(R.drawable.ic_launcher);
            }
        });
    }
}
