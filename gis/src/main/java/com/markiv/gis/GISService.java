package com.markiv.gis;

import android.content.Context;

import com.android.volley.toolbox.ImageLoader;
import com.markiv.gis.api.VolleyProvider;
import com.markiv.gis.image.GISImageView;
import com.markiv.gis.image.LruBitmapCache;

/**
 * The GISService is the Google Image Search API. The service provides all the components needed to
 * get search results and display them - the SearchSession and the GISImageViewFactory.
 * @author vikrambd
 * @since 2/5/15
 */
public class GISService {
    private Context mContext;
    private int mPageSize;
    private GISImageViewFactory mImageViewFactory;

    public GISService(Context context, int pageSize) {
        mContext = context;
        mPageSize = pageSize;
        mImageViewFactory = new GISImageViewFactory();
    }

    /**
     * Start a new search session.
     * @param query
     * @return
     */
    public SearchSession startSearch(String query){
        return SearchSession.newSession(mContext, query, mPageSize);
    }

    /**
     * Returns a factory for creating GISImageViews
     * @return
     */
    public GISImageViewFactory getImageViewFactory(){
        return mImageViewFactory;
    }

    public class GISImageViewFactory {
        private ImageLoader mImageLoader;

        public GISImageViewFactory() {
            mImageLoader = new ImageLoader(VolleyProvider.getInstance(mContext).getImageRequestQueue(), LruBitmapCache.getInstance(mContext));
        }

        public GISImageView newImageView(){
            return new GISImageView(mContext, mImageLoader);
        }
    }
}
