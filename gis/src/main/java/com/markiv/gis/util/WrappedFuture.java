package com.markiv.gis.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class WrappedFuture<T, E> implements Future<T> {
    private final Future<E> mFuture;
    private final Adapter<T, E> mResponseAdapter;

    public WrappedFuture(Future<E> future, Adapter<T, E> responseAdapter) {
        mFuture = future;
        mResponseAdapter = responseAdapter;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return mResponseAdapter.convert(mFuture.get());
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mResponseAdapter.convert(mFuture.get(timeout, unit));
    }

    public static interface Adapter<T, E>{
        public T convert(E result) throws ExecutionException;
    }
}
