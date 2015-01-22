package com.markiv.images.data;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Singleton Volley-provider
 * @author vikrambd
 * @since 1/20/15
 */
public class VolleyProvider {
    private static RequestQueue queue = null;

    private VolleyProvider() { }

    public static synchronized RequestQueue getQueue(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }
}