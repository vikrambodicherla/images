
package com.markiv.gis;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;

import com.markiv.gis.api.GISClient;
import com.markiv.gis.api.VolleyProvider;
import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.api.model.APIResult;

/**
 * Represents a search session. This class abstracts away the notion of pages to provide for a
 * continuous iterative experience over search results. Internally, duplicate requests are merged.
 * Once the session-UI ceases to exist, the session needs to be killed to release all resources at
 * which point it is no longer reusable.
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class SearchSession {
    private static final int DEFAULT_PAGE_SIZE = BuildConfig.MAX_VALID_RSZ;
    private final GISClient mSearchService;
    private final String mQuery;
    private final int mPageSize;

    private final ResultsCache mCache;

    /*
     * We start by assuming we have 64 results. The first time we get a response, we adjust this
     * number if needed
     */
    private boolean resultCountAdjusted = false;
    private int displayedResultCount = BuildConfig.MAX_SEARCH_RESULTS;

    private SearchResultSetUpdateListener mUpdateListener;

    private Handler mMainThreadHandler;

    /**
     * Create a search session for a given query
     * @param searchService
     * @param query
     */
    SearchSession(GISClient searchService, String query){
        this(searchService, query, DEFAULT_PAGE_SIZE);
    }

    /**
     * Create a search session for a given query and a specific pageSize. Optimizing on the pageSize
     * improves the pageSize.
     * @param searchService
     * @param query
     * @param pageSize
     */
    SearchSession(GISClient searchService, String query, int pageSize) {
        mQuery = query;
        mSearchService = searchService;
        mPageSize = pageSize;

        mCache = new ResultsCache();

        mMainThreadHandler = new Handler(Looper.getMainLooper());

        //Prefetch
        //TODO A strategy object can be passed from the UI which takes screen size and device memory
        //TODO into consideration to prefetch better
        fetchResult(0);
    }

    /**
     * Sets the SearchResultSetUpdateListener
     * @param listener
     */
    public void setSearchResultSetUpdatesListener(SearchResultSetUpdateListener listener){
        mUpdateListener = listener;
    }

    /**
     * Get the basis query of the session
     * @return
     */
    public String getQuery() {
        return mQuery;
    }

    /**
     * Get a Future Result object of the search result at a given position
     * @param position
     * @return
     */
    public Future<Result> fetchResult(final int position) {
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
            final Future<APIResponse> apiResponseFuture = mSearchService.fetchPage((position/mPageSize) * mPageSize, mPageSize);
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
                    return mCache.get(position) != null ? new Result(mCache.get(position)) : null;
                }

                @Override
                public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    try {
                        processResponse(apiResponseFuture.get(timeout, unit));
                    }
                    catch (SearchFailedException e){
                        throw new ExecutionException(e);
                    }
                    return mCache.get(position) != null ? new Result(mCache.get(position)) : null;
                }
            };
        }
    }

    /**
     * The number of results in the search set. This can change at any point during the life of the
     * session and the change is notified via the SearchResultSetUpdateListener
     * @return
     */
    public int getResultCount(){
        return displayedResultCount;
    }

    /**
     * Stops the search session. After this no more fetches can be made on the session
     */
    public void stop(){
        mCache.clear();
        mSearchService.stop();
    }

    private void processResponse(APIResponse apiResponse) throws SearchFailedException {
        if (apiResponse.isSuccess()) {
            mCache.batchPut(apiResponse.start, apiResponse.getSearchResults());
            if(!resultCountAdjusted){
                adjustResultCountIfNeeded(apiResponse.responseData.cursor.estimatedResultCount);
            }

            if(apiResponse.start == 0){
                //TODO Optimization: persist this to disk to enable faster loading subsequently
            }
        }
        else {
            throw new SearchFailedException(apiResponse.getErrorMessage());
        }
    }

    private void adjustResultCountIfNeeded(int actualResultCount){
        resultCountAdjusted = true;
        if(actualResultCount < displayedResultCount){
            displayedResultCount = actualResultCount;
            if(mUpdateListener != null){
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mUpdateListener.onResultSetSizeChanged();
                    }
                });
            }
        }
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

    /**
     * A search result from a session. A result composes of the title and a URL.
     */
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

    /**
     * Thrown when a search fails in an unrecoverable fashion
     */
    public static class SearchFailedException extends Exception {
        public SearchFailedException(Throwable cause){
            super(cause);
        }

        public SearchFailedException(String error){
            super(error);
        }
    }

    /**
     * Interface definition for a callback to notify the client of changes in the result set
     */
    public interface SearchResultSetUpdateListener {
        /**
         * This method is invoked when the search result set changes. This invocation happens on
         * the main thread.
         */
        public void onResultSetSizeChanged();
    }
}
