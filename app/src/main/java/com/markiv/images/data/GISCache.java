package com.markiv.images.data;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.support.v4.util.LruCache;

import com.markiv.images.data.model.GISResult;

/**
 * @author vikrambd
 * @since 1/31/15
 */
public class GISCache {
    private final LruCache<Integer, GISResult> mImageSearchResults = new LruCache<>(64);

    public synchronized void put(int position, GISResult result){
        mImageSearchResults.put(position, result);
    }

    public synchronized void batchPut(int startPosition, Iterator<GISResult> results){
        while (results.hasNext()){
            put(startPosition++, results.next());
        }
    }

    public synchronized GISResult get(int position) {
        return mImageSearchResults.get(position);
    }

    public synchronized Future<GISResult> getWrappedInFuture(int position){
        final GISResult result = get(position);
        if(result != null){
            return new Future<GISResult>() {
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
                    return true;
                }

                @Override
                public GISResult get() throws InterruptedException, ExecutionException {
                    return result;
                }

                @Override
                public GISResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return get();
                }
            };
        }
        else {
            return null;
        }
    }

    public synchronized void clear(){
        mImageSearchResults.evictAll();
    }
}
