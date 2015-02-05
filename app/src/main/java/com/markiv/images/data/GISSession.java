
package com.markiv.images.data;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;

import com.markiv.images.data.model.GISResponse;
import com.markiv.images.data.model.GISResult;

/**
 * Represents a search session. This class abstracts away the notion of pages to provide for a
 * continuous iterative experience over search results. The session maintains 3*PEEK_FACTOR blocks
 * of List<GImageSearchResult> corresponding to the current block being viewed, the previous and the
 * next block
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class GISSession {
    private final GISService mSearchService;
    private final String mQuery;
    private final int mPageSize;

    private final GISCache mCache;

    private int mResultCount = -1;


    GISSession(GISService searchService, String query, int pageSize) {
        mQuery = query;
        mSearchService = searchService;
        mPageSize = pageSize;

        mCache = new GISCache();

        //Prefetch
        //TODO A strategy object can be passed from the UI which takes screen size and device memory
        //TODO into consideration to prefetch better
        fetchResult(0);
    }

    public String getQuery() {
        return mQuery;
    }

    public Future<GISResult> fetchResult(int pos) {
        Future<GISResult> cachedResult = mCache.getWrappedInFuture(pos);
        if(cachedResult != null){
            return cachedResult;
        }
        else {
            return buildWrapperResultGetFuture(pos, mSearchService.fetchPage((pos/mPageSize) * mPageSize, mPageSize));
        }
    }

    public void persist(){
        //TODO Implement
    }

    /*
     * This method returns a valid value only after the first call is done successfully.
     * //TODO This is hacky. This call should always work - either return a valid value or notify the
     * //TODO caller that a value is not yet available
     */
    public int getResultCount(){
        return mResultCount;
    }

    public void kill(){
        mCache.clear();
        mSearchService.shutdownNow();
    }

    private Future<GISResult> buildWrapperResultGetFuture(final int position, final Future<GISResponse> searchResponseFuture){
        return new Future<GISResult>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return searchResponseFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return searchResponseFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                //Processing time negligent compared to the network request
                return searchResponseFuture.isDone();
            }

            @Override
            public GISResult get() throws InterruptedException, ExecutionException {
                GISResponse searchResponse = searchResponseFuture.get();
                processResponse(searchResponse);
                return mCache.get(position);
            }

            @Override
            public GISResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }
        };
    }

    private void processResponse(GISResponse response){
        if (response.isSuccess()) {
            mCache.batchPut(response.start, response.getSearchResults());
            if(mResultCount == -1){
                mResultCount = response.responseData.cursor.estimatedResultCount;
            }

            if(response.start == 0){
                //TODO Optimization: persist this to disk to enable faster loading subsequently
            }
        }
    }

    private void persistPage(GISResponse response){

    }

    //If we at any point need more options, we should convert this to a Builder
    public static GISSession newSession(Context context, String query, int pageSize){
        return new GISSession(GISService.newInstance(query, VolleyProvider.getInstance(context).getQueue()), query, pageSize);
    }
}
