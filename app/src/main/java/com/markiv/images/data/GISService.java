package com.markiv.images.data;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.text.format.Formatter;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.markiv.images.data.model.GISResponse;

/**
 * The Google Image Search Service API. This service lets you start a search session via @link GImageSearchService#newSearchSession.
 * @author vikrambd
 * @since 1/20/15
 *
 * //TODO Evaluate if caching is useful
 */
public class GISService {
    private static GISService sGISService;

    private static final Object sREQUEST_LIST_LOCK = new Object();

    private final ExecutorService mExecutors = Executors.newFixedThreadPool(4);
    private final HttpClient mHttpClient = new DefaultHttpClient();
    
    private final ConcurrentHashMap<String, Future<GISResponse>> mInFlightRequests = new ConcurrentHashMap<>();

    private final String mLocalIpAddress;

    //TODO Externalize
    //private static final String sSEARCH_QUERY_URL = BuildConfig.GOOGLE_SEARCH_API;
    private static final String sSEARCH_QUERY_URL = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%1$s&start=%2$s&rsz=%3$s&userip=%4$s&imgsz=small";

    //TODO Overload for testability
    public static synchronized GISService getInstance(){
        if(sGISService == null){
            sGISService = new GISService();
        }
        return sGISService;
    }

    public GISService() {
        mLocalIpAddress = getLocalIpAddress();
    }

    public Future<GISResponse> fetchPage(final String query, final int start, final int rsz) {
        synchronized (sREQUEST_LIST_LOCK) {
            final String requestIdentifier = getRequestIdentifier(query, start, rsz);
            Future<GISResponse> inFlightSearchResponseFuture = mInFlightRequests.get(requestIdentifier);
            if(inFlightSearchResponseFuture != null){
                return inFlightSearchResponseFuture;
            }
            else {
                GISGet get = new GISGet(query, start, rsz);
                final Future<GISResponse> searchResponseFuture = mExecutors.submit(get);

                mInFlightRequests.put(requestIdentifier, searchResponseFuture);
                return new Future<GISResponse>() {
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
                        return searchResponseFuture.isDone();
                    }

                    @Override
                    public GISResponse get() throws InterruptedException, ExecutionException {
                        synchronized (sREQUEST_LIST_LOCK){
                            mInFlightRequests.remove(requestIdentifier);
                        }
                        return searchResponseFuture.get();
                    }

                    @Override
                    public GISResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        synchronized (sREQUEST_LIST_LOCK){
                            mInFlightRequests.remove(requestIdentifier);
                        }
                        return searchResponseFuture.get(timeout, unit);
                    }
                };
            }
        }
    }

    private String getRequestIdentifier(String query, int start, int rsz){
        return query + "S:" + String.valueOf(start) + "R:" + String.valueOf(rsz);
    }

    GISResponse parse(String jsonString, String query, int start, int rsz){
        GISResponse searchResponse = new GsonBuilder().create().fromJson(jsonString, GISResponse.class);
        searchResponse.query = query;
        searchResponse.start = start;
        searchResponse.rsz = rsz;
        return searchResponse;
    }

    class GISGet implements Callable<GISResponse> {
        final String mUrl;

        final String mQuery;
        final int mStart;
        final int mRsz;

        public GISGet(String query, int start, int rsz){
            mUrl = buildUrl(query, start, rsz);

            mQuery = query;
            mStart = start;
            mRsz = rsz;
        }

        @Override
        public GISResponse call() throws Exception {
            HttpGet get = new HttpGet(mUrl);
            HttpResponse response = mHttpClient.execute(get);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                String jsonString = EntityUtils.toString(response.getEntity());
                GISResponse gisResponse = parse(jsonString, mQuery, mStart, mRsz);
                return gisResponse;
            }
            else {
                return null;
            }
        }

        public String buildUrl(String query, int start, int rsz) {
            try {
                return String.format(sSEARCH_QUERY_URL, URLEncoder.encode(query, "utf-8"), String.valueOf(start), String.valueOf(rsz), mLocalIpAddress);
            }
            catch (UnsupportedEncodingException e){
                Log.e("GImageSearchService", "Encoding the query to utf-8 failed", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            return ((o != null) && (o instanceof GISGet))
                    && ((GISGet) o).mUrl.equals(mUrl) && ((GISGet) o).mStart == mStart
                    && ((GISGet) o).mRsz == mRsz;
        }

        @Override
        public String toString() {
            return mUrl;
        }
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
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
}
