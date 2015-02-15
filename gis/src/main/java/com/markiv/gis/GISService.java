package com.markiv.gis;

import android.content.Context;

import com.markiv.gis.api.GISClient;
import com.markiv.gis.api.VolleyRequestQueueProvider;
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

    public GISService(Context context, int pageSize) {
        mContext = context;
        mPageSize = pageSize;
    }

    /**
     * Start a new search session.
     * @param query
     * @return
     */
    public SearchSession startSearch(String query){
        return new SearchSession(GISClient.newInstance(query, VolleyRequestQueueProvider.getInstance(mContext).getRequestQueue()), query, mPageSize);
    }

    /**
     * Returns an ImageManager for creating GISImageViews
     * @return
     */
    public GISImageManager newImageManager(){
        return new GISImageManager(mContext, VolleyRequestQueueProvider.getInstance(mContext).getImageRequestQueue(), LruBitmapCache.getInstance(mContext));
    }
}
