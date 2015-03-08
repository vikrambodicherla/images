
package com.markiv.gis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.volley.VolleyError;
import com.markiv.gis.api.GISClient;
import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.network.ListenableRequestFuture;
import com.markiv.gis.util.CurrentFuture;
import com.markiv.gis.util.WrappedFuture;

/**
 * The Google Image Search Service API. This service lets you fetch a page and get the overall
 * result set size. Internally, it de-duplicates requests and caches pages. Once the service is
 * shutdown, it will not accept anymore requests.
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class GISService {
    private static final int sPAGE_CACHE_SIZE = 5;
    private static final Object sREQUEST_LIST_LOCK = new Object();
    private static final Object sCACHE_LOCK = new Object();

    private final ConcurrentHashMap<String, Future<APIResponse>> mInFlightRequests = new ConcurrentHashMap<>();

    // GISClient doesnt cache requests, so we can pages instead
    private final LruCache<String, Page> mCachedPages = new LruCache<String, Page>(sPAGE_CACHE_SIZE);
    private final GISClient mGISClient;

    private final String mQuery;

    public GISService(String query, GISClient gisClient) {
        mQuery = query;
        mGISClient = gisClient;
    }

    public void cancelAll() {
        mGISClient.cancelAll();
    }

    public void stop() {
        mGISClient.stop();
    }

    public int getMaxPageSize() {
        return BuildConfig.MAX_VALID_RSZ;
    }

    /**
     * Fetch a page
     * @param start
     * @param rsz
     * @return
     * @throws UnsupportedSearchRequestException
     */
    public Future<Page> fetchPage(final int start, final int rsz)
            throws UnsupportedSearchRequestException {
        ensureValidSearchRequest(start, rsz);

        final String requestIdentifier = getRequestIdentifier(mQuery, start, rsz);
        final Page cachedPage;
        synchronized (sCACHE_LOCK) {
            cachedPage = mCachedPages.get(requestIdentifier);
        }

        if (cachedPage != null) {
            return new CurrentFuture<Page>(cachedPage);
        } else {
            Future<APIResponse> apiResponseFuture;
            synchronized (sREQUEST_LIST_LOCK) {
                apiResponseFuture = mInFlightRequests.get(requestIdentifier);
                if (apiResponseFuture == null) {
                    synchronized (sREQUEST_LIST_LOCK) {
                        apiResponseFuture = ListenableRequestFuture
                                .newFuture(new ListenableRequestFuture.ResponseListener() {
                                    @Override
                                    public void onResponse(Object response) {
                                        synchronized (GISService.sREQUEST_LIST_LOCK) {
                                            mInFlightRequests.remove(requestIdentifier);
                                        }
                                    }

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        synchronized (GISService.sREQUEST_LIST_LOCK) {
                                            mInFlightRequests.remove(requestIdentifier);
                                        }
                                    }
                                });
                        mGISClient.newRequest(mQuery, start, rsz, (ListenableRequestFuture)apiResponseFuture, (ListenableRequestFuture)apiResponseFuture);
                        mInFlightRequests.put(requestIdentifier, apiResponseFuture);
                    }
                }
            }

            return new WrappedFuture<Page, APIResponse>(apiResponseFuture,
                    new WrappedFuture.Adapter<Page, APIResponse>() {
                        @Override
                        public Page convert(APIResponse result) throws ExecutionException {
                            try {
                                return buildPageFromApiResponse(start, rsz, result,
                                        requestIdentifier);
                            }
                            catch (SearchFailedException e) {
                                Log.d("GISClient", "Search failed: " + e.getMessage());
                                throw new ExecutionException(e);
                            }
                        }
                    });
        }
    }

    /**
     * Fetch the result set size
     * @return
     */
    public Future<Integer> fetchResultSetSize() {
        if (mCachedPages.size() > 0) {
            // Get the number from an available page
            Page somePage = mCachedPages.snapshot().values().iterator().next();
            return new CurrentFuture<Integer>(somePage.getEstimatedResultCount());
        }
        else {
            // Ask for the first page
            try {
                return new WrappedFuture<Integer, Page>(fetchPage(0, getMaxPageSize()),
                        new WrappedFuture.Adapter<Integer, Page>() {
                            @Override
                            public Integer convert(Page result) throws ExecutionException {
                                // This is an API limitation: As many results as there maybe, the
                                // API only allows 64
                                return Math.min(result.getEstimatedResultCount(),
                                        BuildConfig.MAX_RESULT_SET_SIZE);
                            }
                        });
            } catch (UnsupportedSearchRequestException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void ensureValidSearchRequest(int start, int rsz)
            throws UnsupportedSearchRequestException {
        if (start < BuildConfig.MIN_VALID_START || start > BuildConfig.MAX_VALID_START
                || rsz < BuildConfig.MIN_VALID_RSZ || rsz > BuildConfig.MAX_VALID_RSZ) {
            throw new UnsupportedSearchRequestException("start: " + start + " or rsz: " + rsz
                    + " outside the supported range");
        }
    }

    private Page buildPageFromApiResponse(int start, int count, APIResponse apiResponse,
            String requestKey) throws GISService.SearchFailedException {
        final Page page;
        synchronized (sCACHE_LOCK) {
            if (apiResponse.isSuccess()) {
                page = new Page(start, count, apiResponse);
                mCachedPages.put(requestKey, page);
            }
            else {
                throw new GISService.SearchFailedException(apiResponse.getErrorMessage());
            }
        }
        return page;
    }

    private String getRequestIdentifier(String query, int start, int rsz) {
        return query + "S:" + String.valueOf(start) + "R:" + String.valueOf(rsz);
    }

    public static GISService newInstance(Context context, String query) {
        return new GISService(query, GISClient.newInstance(context));
    }

    /**
     * Thrown when a search fails in an unrecoverable fashion
     */
    public static class SearchFailedException extends Exception {
        public SearchFailedException(Throwable cause) {
            super(cause);
        }

        public SearchFailedException(String error) {
            super(error);
        }
    }

    /**
     * Thrown when an unsupported request is made to the API
     */
    public static class UnsupportedSearchRequestException extends Exception {
        public UnsupportedSearchRequestException(String detailMessage) {
            super(detailMessage);
        }
    }
}
