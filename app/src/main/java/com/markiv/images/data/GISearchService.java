package com.markiv.images.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.GsonBuilder;
import com.markiv.images.data.model.GISearchResponse;

/**
 * The Google Image Search Service API. This service lets you start a search session via @link GImageSearchService#newSearchSession.
 * @author vikrambd
 * @since 1/20/15
 */
public class GISearchService {
    private static GISearchService sGISearchService;

    //TODO Externalize
    //private static final String sSEARCH_QUERY_URL = BuildConfig.GOOGLE_SEARCH_API;
    private static final String sSEARCH_QUERY_URL = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%1$s&start=%2$s&rsz=%3$s";

    private Context mContext;
    private RequestQueue mVolleyRequestQueue;

    //TODO Overload for testability
    public static synchronized GISearchService getInstance(Context context){
        if(sGISearchService == null){
            sGISearchService = new GISearchService(context);
        }
        return sGISearchService;
    }

    private GISearchService(Context context){
        mContext = context;
        mVolleyRequestQueue = VolleyProvider.getQueue(context);
    }

    GISearchResponse blockingQuery(String query, int start, int rsz) throws Exception {
        RequestFuture<GISearchResponse> requestFuture = RequestFuture.newFuture();
        GISearchRequest request = new GISearchRequest(start, rsz, buildUrl(query, start, rsz), requestFuture, requestFuture);
        mVolleyRequestQueue.add(request);

        try {
            return requestFuture.get();
        }
        catch (InterruptedException e) {
            //TODO
        }
        catch (ExecutionException e) {
            //TODO
        }

        return null;
    }

    void asyncQuery(String query, int start, int rsz, Response.Listener<GISearchResponse> listener, Response.ErrorListener errorListener) throws Exception {
        mVolleyRequestQueue.add(new GISearchRequest(start, rsz, buildUrl(query, start, rsz), listener, errorListener));
    }

    public class Builder {
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
    }

    public Builder newSearchSession(String query){
        return new Builder(query);
    }

    private class GISearchRequest extends Request<GISearchResponse>{
        private int start;
        private int rsz;

        private Response.Listener<GISearchResponse> listener;

        private GISearchRequest(int start, int rsz, String url, Response.Listener<GISearchResponse> listener, Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            this.start = start;
            this.rsz = rsz;
            this.listener = listener;
        }

        @Override
        protected Response<GISearchResponse> parseNetworkResponse(NetworkResponse response) {
            String jsonString;
            try {
                jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            }
            catch (UnsupportedEncodingException e){
                Log.e("GImageSearchService", "Unable to parse server-response, encoding not supported", e);
                return null;
            }

            GISearchResponse searchResponse = new GsonBuilder().create().fromJson(jsonString, GISearchResponse.class);
            searchResponse.start = start;
            searchResponse.rsz = rsz;
            return Response.success(searchResponse, HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(GISearchResponse response) {
            listener.onResponse(response);
        }
    }

    private static String buildUrl(String query, int start, int rsz) throws Exception {
        String url = null;
        try {
            url = String.format(sSEARCH_QUERY_URL, URLEncoder.encode(query, "utf-8"), String.valueOf(start), String.valueOf(rsz));
        }
        catch (UnsupportedEncodingException e){
            Log.e("GImageSearchService", "Encoding the query to utf-8 failed", e);
            throw new Exception("");
        }

        return url;
    }
}
