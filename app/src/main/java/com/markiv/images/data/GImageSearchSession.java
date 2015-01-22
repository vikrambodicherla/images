
package com.markiv.images.data;

import java.util.Iterator;

import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.markiv.images.data.model.GISearchResponse;
import com.markiv.images.data.model.GISearchResult;

/**
 * Represents a search session. This class abstracts away the notion of pages to provide for a
 * continuous iterative experience over search results. The session maintains 3*PEEK_FACTOR blocks
 * of List<GImageSearchResult> corresponding to the current block being viewed, the previous and the
 * next block
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class GImageSearchSession implements Response.Listener<GISearchResponse>,
        Response.ErrorListener {
    private static final int LRU_CACHE_SIZE = 10;

    private final GISearchService mSearchService;
    private final String mQuery;
    private final int mPageSize;

    private final Object LRC_CACHE_LOCK = new Object();
    private final LruCache<Integer, GISearchResult> mImageSearchResults = new LruCache<Integer, GISearchResult>(
            LRU_CACHE_SIZE);

    GImageSearchSession(GISearchService searchService, String query, int pageSize) {
        mQuery = query;
        mSearchService = searchService;
        mPageSize = pageSize;

        //asyncGetBlock(0);
    }

    private void blockingGetBlock(int start) {
        try {
            GISearchResponse response = mSearchService.blockingQuery(mQuery, start, mPageSize);
            onResponse(response);
        } catch (Exception e) {
            // TODO
        }
    }

    private void asyncGetBlock(int start) {
        try {
            mSearchService.asyncQuery(mQuery, start, mPageSize, this, this);
        } catch (Exception e) {
            // TODO
        }
    }

    public GISearchResult blockingGetSearchResult(int position) {
        GISearchResult result = null;
        synchronized (LRC_CACHE_LOCK) {
            result = mImageSearchResults.get(position);
        }
        if (result == null) {
            blockingGetBlock(position/mPageSize);
        }
        return result;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        // TODO Get failed URL
        Log.e("GImageSearchSession", "Failed to fetch data: " + error.networkResponse.statusCode);
    }

    @Override
    public void onResponse(GISearchResponse response) {
        if (response.isSuccess()) {
            Iterator<GISearchResult> results = response.getSearchResults();
            int start = response.start;

            synchronized (LRC_CACHE_LOCK) {
                while (results.hasNext()) {
                    mImageSearchResults.put(start++, results.next());
                }
            }
        }
    }
}
