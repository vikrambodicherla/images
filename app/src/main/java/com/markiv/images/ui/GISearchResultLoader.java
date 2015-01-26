package com.markiv.images.ui;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.markiv.images.data.GImageSearchSession;
import com.markiv.images.data.model.GISearchResponse;
import com.markiv.images.data.model.GISearchResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author vikrambd
 * @since 1/25/15
 */
public class GISearchResultLoader implements Future<GISearchResult> {

    public GISearchResultLoader(GImageSearchSession searchSession, int position) {
        Future<GISearchResponse> response = searchSession.blockingGetSearchResult(position);

        searchSession.getSearchResult(position, new Response.Listener<GISearchResult>() {
            @Override
            public void onResponse(GISearchResult response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

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
        return false;
    }

    @Override
    public GISearchResult get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public GISearchResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public GISearchResultLoader createLoader(GImageSearchSession searchSession, int position){
        return new GISearchResultLoader(searchSession, position);
    }
}
