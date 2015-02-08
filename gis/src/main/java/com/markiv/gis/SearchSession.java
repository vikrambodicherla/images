
package com.markiv.gis;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.support.v4.util.LruCache;

import com.markiv.gis.api.GISClient;
import com.markiv.gis.api.VolleyProvider;
import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.api.model.APIResult;

/**
 * Represents a search session. This class abstracts away the notion of pages to provide for a
 * continuous iterative experience over search results. The session maintains 3*PEEK_FACTOR blocks
 * of List<GImageSearchResult> corresponding to the current block being viewed, the previous and the
 * next block
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class SearchSession {
    private final GISClient mSearchService;
    private final String mQuery;
    private final int mPageSize;

    private final ResultsCache mCache;

    private int mResultCount = -1;

    SearchSession(GISClient searchService, String query, int pageSize) {
        mQuery = query;
        mSearchService = searchService;
        mPageSize = pageSize;

        mCache = new ResultsCache();

        //Prefetch
        //TODO A strategy object can be passed from the UI which takes screen size and device memory
        //TODO into consideration to prefetch better
        fetchResult(0);
    }

    public String getQuery() {
        return mQuery;
    }

    public Future<Result> fetchResult(final int pos) {
        final APIResult cachedResult = mCache.get(pos);
        if(cachedResult != null){
            return new Future<Result>() {
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
                    return true;
                }

                @Override
                public Result get() throws InterruptedException, ExecutionException {
                    return new Result(cachedResult);
                }

                @Override
                public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return new Result(cachedResult);
                }
            };
        }
        else {
            final Future<APIResponse> apiResponseFuture = mSearchService.fetchPage((pos/mPageSize) * mPageSize, mPageSize);
            return new Future<Result>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return apiResponseFuture.cancel(mayInterruptIfRunning);
                }

                @Override
                public boolean isCancelled() {
                    return apiResponseFuture.isCancelled();
                }

                @Override
                public boolean isDone() {
                    return apiResponseFuture.isDone();
                }

                @Override
                public Result get() throws InterruptedException, ExecutionException {
                    try {
                        processResponse(apiResponseFuture.get());
                    }
                    catch (SearchFailedException e){
                        throw new ExecutionException(e);
                    }
                    return mCache.get(pos) != null ? new Result(mCache.get(pos)) : null;
                }

                @Override
                public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    try {
                        processResponse(apiResponseFuture.get(timeout, unit));
                    }
                    catch (SearchFailedException e){
                        throw new ExecutionException(e);
                    }
                    return mCache.get(pos) != null ? new Result(mCache.get(pos)) : null;
                }
            };
        }
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

    private void processResponse(APIResponse apiResponse) throws SearchFailedException {
        if (apiResponse.isSuccess()) {
            mCache.batchPut(apiResponse.start, apiResponse.getSearchResults());
            if(mResultCount == -1){
                mResultCount = apiResponse.responseData.cursor.estimatedResultCount;
            }

            if(apiResponse.start == 0){
                //TODO Optimization: persist this to disk to enable faster loading subsequently
            }
        }
        else {
            throw new SearchFailedException(apiResponse.getErrorMessage());
        }
    }

    //If we at any point need more options, we should convert this to a Builder
    public static SearchSession newSession(Context context, String query, int pageSize){
        return new SearchSession(GISClient.newInstance(query, VolleyProvider.getInstance(context).getQueue()), query, pageSize);
    }

    public static class ResultsCache {
        private final LruCache<Integer, APIResult> mImageSearchResults = new LruCache<>(64);

        public synchronized void put(int position, APIResult APIResult){
            mImageSearchResults.put(position, APIResult);
        }

        public synchronized void batchPut(int startPosition, Iterator<APIResult> results){
            while (results.hasNext()){
                put(startPosition++, results.next());
            }
        }

        public synchronized APIResult get(int position) {
            return mImageSearchResults.get(position);
        }

        public synchronized void clear(){
            mImageSearchResults.evictAll();
        }
    }

    public static class Result {
        public APIResult mAPIResult;

        public Result(APIResult APIResult) {
            mAPIResult = APIResult;
        }

        public String getTitle(){
            return mAPIResult.getTitleNoFormatting();
        }

        public String getUrl(){
            return mAPIResult.getTbUrl();
        }
    }

    public static class SearchFailedException extends Exception {
        public SearchFailedException(Throwable cause){
            super(cause);
        }

        public SearchFailedException(String error){
            super(error);
        }
    }
}
