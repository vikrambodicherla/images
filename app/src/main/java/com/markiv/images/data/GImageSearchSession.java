
package com.markiv.images.data;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.support.v4.util.LruCache;

import com.android.volley.Response;
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
public class GImageSearchSession {
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

        //Pre-fetch the first block by asking for the 0th position
        getSearchResult(0, null, null);
    }

    public Future<GISearchResult> blockingGetSearchResult2(int position) {
        return new Future<GISearchResult>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public GISearchResult get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public GISearchResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    public GISearchResult blockingGetSearchResult(int position) {
        GISearchResult result = null;
        synchronized (LRC_CACHE_LOCK) {
            result = mImageSearchResults.get(position);
        }
        if (result == null) {
            try {
                GISearchResponse response = mSearchService.blockingQuery(mQuery, position / mPageSize, mPageSize);
                processResponse(response);
            }
            catch (Exception e){
                //TODO
            }
            result = mImageSearchResults.get(position);
        }
        return result;
    }

    public void getSearchResult(final int position, final Response.Listener<GISearchResult> searchResultListener, final Response.ErrorListener errorListener) {
        GISearchResult result = null;
        synchronized (LRC_CACHE_LOCK) {
            result = mImageSearchResults.get(position);
        }
        if (result == null) {
            try {
                mSearchService.asyncQuery(mQuery, position/mPageSize, mPageSize, new Response.Listener<GISearchResponse>() {
                    @Override
                    public void onResponse(GISearchResponse response) {
                        processResponse(response);
                        if(searchResultListener != null) {
                            searchResultListener.onResponse(mImageSearchResults.get(position));
                        }
                    }
                }, errorListener);
                //GISearchResponse response = mSearchService.blockingQuery(mQuery, position / mPageSize, mPageSize);
                //onResponse(response);
            }
            catch (Exception e){
                //TODO
            }
            //result = mImageSearchResults.get(position);
        }
        else {
            searchResultListener.onResponse(mImageSearchResults.get(position));
        }
        //return result;
    }

    private void processResponse(GISearchResponse response){
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
