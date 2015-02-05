
package com.markiv.images.data;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import android.text.format.Formatter;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.GsonBuilder;
import com.markiv.images.BuildConfig;
import com.markiv.images.data.model.GISResponse;

/**
 * The Google Image Search Service API. This service lets you fetch a page via fetchPage.
 * Internally, it merges duplicate requests. Once the service is shutdown, it will not accept
 * anymore requests
 * 
 * @author vikrambd
 * @since 1/20/15 //TODO Evaluate if caching is useful. Volley gives us free caching.
 */
public class GISService {
    private static final Object sREQUEST_LIST_LOCK = new Object();
    private static final String sSEARCH_QUERY_URL = BuildConfig.GOOGLE_SEARCH_API;

    private final ConcurrentHashMap<String, Future<GISResponse>> mInFlightRequests = new ConcurrentHashMap<>();
    private final RequestQueue mRequestQueue;

    private final String mQuery;

    private final String mLocalIpAddress;

    public GISService(String query, RequestQueue requestQueue) {
        mQuery = query;
        mLocalIpAddress = getLocalIpAddress();
        mRequestQueue = requestQueue;
    }

    public void shutdownNow() {
        mRequestQueue.stop();
    }

    public Future<GISResponse> fetchPage(final int start, final int rsz) {
        synchronized (sREQUEST_LIST_LOCK) {
            final String requestIdentifier = getRequestIdentifier(mQuery, start, rsz);
            Future<GISResponse> inFlightSearchResponseFuture = mInFlightRequests
                    .get(requestIdentifier);
            if (inFlightSearchResponseFuture != null) {
                return inFlightSearchResponseFuture;
            } else {
                RequestFuture<GISResponse> future = buildNewRequest(start, rsz);
                mInFlightRequests.put(requestIdentifier, future);
                return future;
            }
        }
    }

    private String getRequestIdentifier(String query, int start, int rsz) {
        return query + "S:" + String.valueOf(start) + "R:" + String.valueOf(rsz);
    }

    GISResponse parse(String jsonString, String query, int start, int rsz) {
        GISResponse searchResponse = new GsonBuilder().create().fromJson(jsonString,
                GISResponse.class);
        searchResponse.query = query;
        searchResponse.start = start;
        searchResponse.rsz = rsz;
        return searchResponse;
    }

    class GISGetRequest extends Request<GISResponse> {
        final String mUrl;

        final String mQuery;
        final int mStart;
        final int mRsz;
        final Response.Listener<GISResponse> mListener;

        GISGetRequest(String url, String query, int start, int rsz, Response.ErrorListener errorListener, Response.Listener<GISResponse> listener) {
            super(Method.GET, url, errorListener);

            mListener = listener;
            mUrl = url;
            mQuery = query;
            mStart = start;
            mRsz = rsz;
        }

        @Override
        protected Response<GISResponse> parseNetworkResponse(NetworkResponse response) {
            try {
                String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(parse(json, mQuery, mStart, mRsz), HttpHeaderParser.parseCacheHeaders(response));
            }
            catch (UnsupportedEncodingException e){
                return Response.error(new VolleyError(e));
            }
        }

        @Override
        protected void deliverResponse(GISResponse response) {
            mListener.onResponse(response);
            synchronized (sREQUEST_LIST_LOCK) {
                mInFlightRequests.remove(getRequestIdentifier(mQuery, mStart, mRsz));
            }
        }

        @Override
        public boolean equals(Object o) {
            return ((o != null) && (o instanceof GISGetRequest))
                    && ((GISGetRequest) o).mUrl.equals(mUrl) && ((GISGetRequest) o).mStart == mStart
                    && ((GISGetRequest) o).mRsz == mRsz;
        }
    }

    RequestFuture<GISResponse> buildNewRequest(final int start, final int rsz){
        RequestFuture<GISResponse> future = RequestFuture.newFuture();
        GISGetRequest gisGetRequest = new GISGetRequest(buildUrl(mQuery, start, rsz), mQuery, start, rsz, future, future);
        mRequestQueue.add(gisGetRequest);
        return future;
    }

    public String buildUrl(String query, int start, int rsz) {
        try {
            return String.format(sSEARCH_QUERY_URL, URLEncoder.encode(query, "utf-8"),
                    String.valueOf(start), String.valueOf(rsz), mLocalIpAddress);
        } catch (UnsupportedEncodingException e) {
            Log.e("GImageSearchService", "Encoding the query to utf-8 failed", e);
            throw new RuntimeException(e);
        }
    }

    public String getLocalIpAddress() {
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

    public static GISService newInstance(String query, RequestQueue requestQueue){
        return new GISService(query, requestQueue);
    }
}
