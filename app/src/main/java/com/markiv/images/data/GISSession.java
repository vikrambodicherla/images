
package com.markiv.images.data;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    GISSession(GISService searchService, String query, int pageSize) {
        mQuery = query;
        mSearchService = searchService;
        mPageSize = pageSize;

        mCache = new GISCache();
    }

    public Future<GISResult> fetchResult(int pos) {
        Future<GISResult> cachedResult = mCache.getWrappedInFuture(pos);
        if(cachedResult != null){
            return cachedResult;
        }
        else {
            return buildWrapperResultGetFuture(pos, mSearchService.fetchPage(mQuery, (pos/mPageSize) * mPageSize, mPageSize));
        }
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
        }
    }

    //If we at anypoint need more options, we should convert this to a Builder
    public static GISSession newSession(String query, int pageSize){
        return new GISSession(GISService.getInstance(), query, pageSize);
    }

        /*public class Builder {
        private String query;
        private int pageSize;

        public Builder(String query) {
            this.query = query;
        }

        public Builder withPageSize(int pageSize){
            this.pageSize = pageSize;
            return this;
        }

        public GImageSearchSession start(){
            if(pageSize > 4 || pageSize < 0){
                throw new IllegalArgumentException("Invalid page size");
            }

            return new GImageSearchSession(GISearchService.this, query, pageSize);
        }
    }*/
}
