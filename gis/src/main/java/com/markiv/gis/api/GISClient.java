package com.markiv.gis.api;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Enumeration;

import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.markiv.gis.BuildConfig;
import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.network.VolleyProvider;

/**
 * The GISClient manages the network stack and can be used to make a call to the API for a given query, start and rsz. There is no de-dup of requests
 * and no caching support (since GIS wont let Volley cache)
 * @author vikrambd
 * @since 3/7/15
 */
public class GISClient {
    private static final String sLOCAL_IP;
    private static final String sSEARCH_QUERY_URL = BuildConfig.GOOGLE_SEARCH_API;

    private final RequestQueue mRequestQueue;

    public GISClient(Context context, VolleyProvider volleyProvider) {
        mRequestQueue = volleyProvider.newRequestQueue(context);
    }

    public GISGetRequest newRequest(final String query, final int start, final int rsz, Response.Listener<APIResponse> listener, Response.ErrorListener errorListener) {
        GISGetRequest gisGet = new GISGetRequest(buildUrl(query, start, rsz), query,
                start, rsz, listener, errorListener);
        mRequestQueue.add(gisGet);
        return gisGet;
    }

    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public void stop(){
        mRequestQueue.stop();
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

    static {
        sLOCAL_IP = getLocalIpAddress();
    }

    public static GISClient newInstance(Context context){
        return new GISClient(context, VolleyProvider.getInstance());
    }
}