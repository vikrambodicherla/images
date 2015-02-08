
package com.markiv.gis;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.support.v4.util.LruCache;
import android.util.Log;

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
        try {
            fetchResult(0);
        }
        catch (InterruptedException e){
            Log.e("SeachSession", "Prefetch interrupted", e);
        }
        catch (SearchFailedException e){
            Log.e("SeachSession", "Prefetch failed", e);
        }
    }

    public String getQuery() {
        return mQuery;
    }

    public Future<Result> fetchResult(final int position) throws InterruptedException, SearchFailedException {
        final APIResult cachedResult = mCache.get(position);
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
            final Future<APIResponse> apiResponseFuture = mSearchService.fetchPage((position / mPageSize) * mPageSize, mPageSize);
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
                        final APIResponse apiResponse = apiResponseFuture.get();
                        processApiResponse(apiResponse);
                    }
                    catch (ExecutionException e){
                        Log.e("SearchSession", "buildResult: Error in getTitle()", e);
                        throw new SearchFailedException(e);
                    }
                    return new Result(mCache.get(position));
                }

                @Override
                public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    try {
                        final APIResponse apiResponse = apiResponseFuture.get(timeout, unit);
                        processApiResponse(apiResponse);
                    }
                    catch (ExecutionException e){
                        Log.e("SearchSession", "buildResult: Error in getTitle()", e);
                        throw new SearchFailedException(e);
                    }
                    return new Result(mCache.get(position));
                }
            };
        }
    }

    private void processApiResponse(APIResponse apiResponse) throws SearchFailedException, InterruptedException {
        if (apiResponse.isSuccess()) {
            mCache.batchPut(apiResponse.start, apiResponse.getSearchResults());
        }
        else {
            throw new SearchFailedException(apiResponse.getErrorMessage());
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
        private final APIResult mAPIResult;

        public Result(APIResult APIResult) {
            mAPIResult = APIResult;
        }

        public String getTitle() {
            return mAPIResult.getTitleNoFormatting();
        }

        public String getUrl() {
            return mAPIResult.getTbUrl();
        }

        @Override
        public String toString() {
            return mAPIResult.toString();
        }
    }

    public class SearchFailedException extends ExecutionException {
        public SearchFailedException(String error){
            super(error);
        }

        public SearchFailedException(Exception e){
            super(e);
        }
    }
}
