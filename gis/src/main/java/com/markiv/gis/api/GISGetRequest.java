package com.markiv.gis.api;

import java.io.UnsupportedEncodingException;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.GsonBuilder;
import com.markiv.gis.api.model.APIResponse;

/**
* @author vikrambd
* @since 3/7/15
*/
public class GISGetRequest extends Request<APIResponse> {
    final String mUrl;

    final String mQuery;
    final int mStart;
    final int mRsz;
    final Response.Listener mListener;

    public GISGetRequest(String url, String query, int start, int rsz, Response.Listener listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        mUrl = url;
        mQuery = query;
        mStart = start;
        mRsz = rsz;

        mListener = listener;
    }

    @Override
    protected com.android.volley.Response parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return com.android.volley.Response.success(parse(json, mQuery, mStart, mRsz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return com.android.volley.Response.error(new VolleyError(e));
        }
    }

    @Override
    protected void deliverResponse(APIResponse APIResponse) {
        mListener.onResponse(APIResponse);
    }

    @Override
    public boolean equals(Object o) {
        return ((o != null) && (o instanceof GISGetRequest))
                && ((GISGetRequest) o).mUrl.equals(mUrl)
                && ((GISGetRequest) o).mStart == mStart
                && ((GISGetRequest) o).mRsz == mRsz;
    }

    APIResponse parse(String jsonString, String query, int start, int rsz) {
        APIResponse searchAPIResponse = new GsonBuilder().create().fromJson(jsonString,
                APIResponse.class);
        searchAPIResponse.query = query;
        searchAPIResponse.start = start;
        searchAPIResponse.rsz = rsz;
        return searchAPIResponse;
    }
}
