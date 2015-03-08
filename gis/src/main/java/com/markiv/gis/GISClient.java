
package com.markiv.gis;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.support.v4.util.LruCache;
import android.text.format.Formatter;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.markiv.gis.api.GISGetRequest;
import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.network.ListenableRequestFuture;
import com.markiv.gis.network.VolleyProvider;
import com.markiv.gis.util.CurrentFuture;
import com.markiv.gis.util.WrappedFuture;

/**
 * The Google Image Search Service API. This service lets you fetch a page via fetchPage.
 * Internally, it merges duplicate requests. Once the service is shutdown, it will not accept
 * anymore requests.
 * 
 * @author vikrambd
 * @since 1/20/15
 */
public class GISClient {
    private static final int sPAGE_CACHE_SIZE = 5;
    private static final String sLOCAL_IP;
    private static final Object sREQUEST_LIST_LOCK = new Object();
    private static final Object sCACHE_LOCK = new Object();
    private static final String sSEARCH_QUERY_URL = BuildConfig.GOOGLE_SEARCH_API;

    private final ConcurrentHashMap<String, Future<APIResponse>> mInFlightRequests = new ConcurrentHashMap<>();

    // We would have wanted to rely on Volley's caching, but we cannot because the server returns a
    // no-cache
    private final LruCache<String, Page> mCachedPages = new LruCache<String, Page>(sPAGE_CACHE_SIZE);
    private final RequestQueue mRequestQueue;

    private final String mQuery;

    public GISClient(Context context, String query, VolleyProvider volleyProvider) {
        mQuery = query;
        mRequestQueue = volleyProvider.newRequestQueue(context);
    }

    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public void stop() {
        mRequestQueue.stop();
    }

    public int getMaxPageSize(){
        return BuildConfig.MAX_VALID_RSZ;
    }

    public Future<Page> fetchPage(final int start, final int rsz) throws UnsupportedSearchRequestException {
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
                    apiResponseFuture = buildNewRequest(start, rsz, requestIdentifier);
                    mInFlightRequests.put(requestIdentifier, apiResponseFuture);
                }
            }

            return new WrappedFuture<Page, APIResponse>(apiResponseFuture, new WrappedFuture.Adapter<Page, APIResponse>() {
                @Override
                public Page convert(APIResponse result) throws ExecutionException {
                    try {
                        return buildPageFromApiResponse(start, rsz, result, requestIdentifier);
                    }
                    catch (SearchFailedException e){
                        Log.d("GISClient", "Search failed: " + e.getMessage());
                        throw new ExecutionException(e);
                    }
                }
            });
        }
    }

    public Future<Integer> fetchEstimatedResultCount(){
        if(mCachedPages.size() > 0){
            //Get the number from an available page
            Page somePage = mCachedPages.snapshot().values().iterator().next();
            return new CurrentFuture<Integer>(somePage.getEstimatedResultCount());
        }
        else {
            //Ask for the first page
            try {
                return new WrappedFuture<Integer, Page>(fetchPage(0, getMaxPageSize()), new WrappedFuture.Adapter<Integer, Page>() {
                    @Override
                    public Integer convert(Page result) throws ExecutionException {
                        //This is an API limitation: As many results as there maybe, the API only allows 64
                        return Math.min(result.getEstimatedResultCount(), BuildConfig.MAX_RESULT_SET_SIZE);
                    }
                });
            }
            catch (UnsupportedSearchRequestException e){
                throw new RuntimeException(e);
            }
        }
    }

    public void ensureValidSearchRequest(int start, int rsz) throws UnsupportedSearchRequestException {
        if(start < BuildConfig.MIN_VALID_START || start > BuildConfig.MAX_VALID_START || rsz < BuildConfig.MIN_VALID_RSZ || rsz > BuildConfig.MAX_VALID_RSZ){
            throw new UnsupportedSearchRequestException("start: " + start + " or rsz: " + rsz + " outside the supported range");
        }
    }

    private Page buildPageFromApiResponse(int start, int count, APIResponse apiResponse,
            String requestKey) throws GISClient.SearchFailedException {
        final Page page;
        synchronized (sCACHE_LOCK) {
            if (apiResponse.isSuccess()) {
                page = new Page(start, count, apiResponse);
                mCachedPages.put(requestKey, page);
            }
            else {
                throw new GISClient.SearchFailedException(apiResponse.getErrorMessage());
            }
        }
        return page;
    }

    private String getRequestIdentifier(String query, int start, int rsz) {
        return query + "S:" + String.valueOf(start) + "R:" + String.valueOf(rsz);
    }

    Future<APIResponse> buildNewRequest(final int start, final int rsz,
            final String requestIdentifier) {
        synchronized (sREQUEST_LIST_LOCK) {
            ListenableRequestFuture<APIResponse> requestFuture = ListenableRequestFuture
                    .newFuture(new ListenableRequestFuture.ResponseListener() {
                        @Override
                        public void onResponse(Object response) {
                            synchronized (GISClient.sREQUEST_LIST_LOCK) {
                                mInFlightRequests.remove(requestIdentifier);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            synchronized (GISClient.sREQUEST_LIST_LOCK) {
                                mInFlightRequests.remove(requestIdentifier);
                            }
                        }
                    });

            GISGetRequest gisGet = new GISGetRequest(buildUrl(mQuery, start, rsz), mQuery,
                    start, rsz, requestFuture, requestFuture);

            mRequestQueue.add(gisGet);
            return requestFuture;
        }
    }

    public String buildUrl(String query, int start, int rsz) {
        try {
            return String.format(sSEARCH_QUERY_URL, URLEncoder.encode(query, "utf-8"),
                    String.valueOf(start), String.valueOf(rsz), sLOCAL_IP);
        } catch (UnsupportedEncodingException e) {
            Log.e("GImageSearchService", "Encoding the query to utf-8 failed", e);
            throw new RuntimeException(e);
        }
    }

    private static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ip = Formatter.formatIpAddress(inetAddress.hashCode());
                        return ip;
                    }
                }
            }
        } catch (SocketException ex) {
            //
        }
        return null;
    }

    public static GISClient newInstance(Context context, String query) {
        return new GISClient(context, query, VolleyProvider.getInstance());
    }

    static {
        sLOCAL_IP = getLocalIpAddress();
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
