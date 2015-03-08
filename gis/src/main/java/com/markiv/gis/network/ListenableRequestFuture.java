package com.markiv.gis.network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class ListenableRequestFuture<T> implements Future<T>, Response.Listener<T>,
        Response.ErrorListener {
    private Request<?> mRequest;
    private boolean mResultReceived = false;
    private T mResult;
    private VolleyError mException;
    private final ResponseListener<T> mResponseListener;

    public static <E> ListenableRequestFuture<E> newFuture(ResponseListener responseListener) {
        return new ListenableRequestFuture<E>(responseListener);
    }

    private ListenableRequestFuture(ResponseListener<T> responseListener) {
        mResponseListener = responseListener;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (mRequest == null) {
            return false;
        }

        if (!isDone()) {
            mRequest.cancel();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(TimeUnit.MILLISECONDS.convert(timeout, unit));
    }

    private synchronized T doGet(Long timeoutMs)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (mException != null) {
            throw new ExecutionException(mException);
        }

        if (mResultReceived) {
            return mResult;
        }

        if (timeoutMs == null) {
            wait(0);
        } else if (timeoutMs > 0) {
            wait(timeoutMs);
        }

        if (mException != null) {
            throw new ExecutionException(mException);
        }

        if (!mResultReceived) {
            throw new TimeoutException();
        }

        return mResult;
    }

    @Override
    public boolean isCancelled() {
        if (mRequest == null) {
            return false;
        }
        return mRequest.isCanceled();
    }

    @Override
    public synchronized boolean isDone() {
        return mResultReceived || mException != null || isCancelled();
    }

    @Override
    public synchronized void onResponse(T response) {
        mResultReceived = true;
        mResult = response;
        notifyAll();
    }

    @Override
    public synchronized void onErrorResponse(VolleyError error) {
        mException = error;
        notifyAll();
    }

    public static interface ResponseListener<T> {
        public void onResponse(T response);
        public void onErrorResponse(VolleyError error);
    }
}
