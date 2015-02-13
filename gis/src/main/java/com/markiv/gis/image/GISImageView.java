package com.markiv.gis.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.markiv.gis.SearchSession;

/**
 * A GISImageView can be loaded with a SearchSession Result and the bitmap is fetched and loaded
 * automatically
 * 
 * @author vikrambd
 * @since 2/4/15
 */
public class GISImageView extends NetworkImageView {
    private static final int FADE_IN_TIME_MS = 250;
    private final ImageLoader mImageLoader;
    private BitmapLoadedListener mBitmapLoadedListener;

    public GISImageView(Context context, ImageLoader imageLoader) {
        super(context);
        mImageLoader = imageLoader;

        setAdjustViewBounds(true);
        setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public void setGISResult(SearchSession.Result result){
        setImageUrl(result.getUrl(), mImageLoader);
    }

    public void setBitmapLoadedListener(BitmapLoadedListener listener){
        mBitmapLoadedListener = listener;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                new ColorDrawable(android.R.color.transparent),
                new BitmapDrawable(getContext().getResources(), bm)
        });

        setImageDrawable(td);
        td.startTransition(FADE_IN_TIME_MS);

        if(mBitmapLoadedListener != null){
            mBitmapLoadedListener.onBitmapLoaded();
        }
    }

    public static interface BitmapLoadedListener {
        public void onBitmapLoaded();
    }
}
