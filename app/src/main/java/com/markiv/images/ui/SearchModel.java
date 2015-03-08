
package com.markiv.images.ui;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.markiv.gis.GISClient;
import com.markiv.gis.Page;
import com.markiv.gis.util.CurrentFuture;
import com.markiv.gis.util.WrappedFuture;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class SearchModel {
    private final GISClient mGISClient;

    private final ResultsCache mCache;

    private int mDisplayedResultCount = 0;
    private final int mPageSize;

    private StateChangeListener mStateChangeListener;
    private State mState = State.EMPTY_SET;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Create a search session for a given query and a specific pageSize. Optimizing on the pageSize
     * improves the pageSize.
     * 
     * @param gisClient
     */
    SearchModel(GISClient gisClient) {
        mGISClient = gisClient;
        mCache = new ResultsCache();

        //TODO: We need a better strategy here
        mPageSize = mGISClient.getMaxPageSize();

        //Fetch count. While we can optimize the hell out of this by getting this info via the first call
        //this approach is cleaner and easier to test
        new AsyncTask<Void, Void, Integer>(){
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mGISClient.fetchEstimatedResultCount().get();
                }
                catch (ExecutionException e){
                    //Failed search
                    Log.d("SearchModel", "Failed get result count", e);
                    return null;
                }
                catch (InterruptedException e){
                    //We cannot handle a failure here
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void onPostExecute(Integer integer) {
                if(integer != null) {
                    mDisplayedResultCount = integer;
                    if (mDisplayedResultCount == 0) {
                        mState = State.EMPTY_SET;
                        //No notification needed to listeners
                    } else {
                        mState = State.NON_EMPTY_SET;
                        if(mStateChangeListener != null){
                            mStateChangeListener.onResultSetSizeChanged(mDisplayedResultCount);
                        }
                    }
                }
                else {
                    mDisplayedResultCount = -1;
                    mState = State.ERROR;
                    if(mStateChangeListener != null){
                        mStateChangeListener.onError(null);
                    }
                }
            }
        }.execute((Void)null);
    }

    public void setStateChangeListener(StateChangeListener stateChangeListener) {
        mStateChangeListener = stateChangeListener;
    }

    public void onPause(){
        mGISClient.cancelAll();
    }

    public void onDestroy(){
        mGISClient.stop();
        mCache.clear();
        mStateChangeListener = null;
    }

    public void onLowMemory(){
        mGISClient.cancelAll();
        mCache.clear();
    }

    /**
     * Get a Future Result object of the search result at a given position
     * 
     * @param position
     * @return
     */
    public Future<Page.Item> fetchResult(final int position) {
        final Page.Item cachedResult = mCache.get(position);
        if (cachedResult != null) {
            return new CurrentFuture<Page.Item>(cachedResult);
        }
        else {
            try {
                return new WrappedFuture<Page.Item, Page>(mGISClient.fetchPage(
                        getPageNumber(position), mPageSize),
                        new WrappedFuture.Adapter<Page.Item, Page>() {
                            @Override
                            public Page.Item convert(Page result) throws ExecutionException {
                                try {
                                    processResponse(result);
                                }
                                catch (final GISClient.SearchFailedException e){
                                    mDisplayedResultCount = -1;
                                    mState = State.ERROR;
                                    if(mStateChangeListener != null) {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mStateChangeListener.onError(e.getMessage());
                                            }
                                        });
                                    }
                                }

                                //TODO I don't like sending a possible null here
                                return mCache.get(position);
                            }
                        });
            } catch (GISClient.UnsupportedSearchRequestException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int getPageNumber(int position){
        return (position/mPageSize) * mPageSize;
    }

    /**
     * The number of results in the search set. This can change at any point during the life of the
     * session and the change is notified via the SearchResultSetUpdateListener
     * 
     * @return
     */
    public int getResultCount() {
        return mDisplayedResultCount;
    }

    /**
     * Cancels all pending requests. This is usually done in onPause
     */
    public void cancelAll() {
        mGISClient.cancelAll();
    }

    /**
     * Stops the GISClient. After this, the SearchSession is no longer usable. This is usually done
     * in onDestroy() of the host activity/fragment
     */
    public void stop() {
        mGISClient.stop();
    }

    /*
     * Process the page and cache the response.
     */
    private void processResponse(Page page) throws GISClient.SearchFailedException {
        if (!page.isEmpty()) {
            mCache.batchPut(page.getStart(), page.getItems().iterator());
        }
        else {
            Log.e("SearchModel", "Asked for page for position: " + page.getStart()
                    + " and got empty. Should not have asked given size is " + getResultCount());
        }
    }

    public State getState() {
        return mState;
    }

    public static class ResultsCache {
        private final LruCache<Integer, Page.Item> mImageSearchResults = new LruCache<>(64);

        public synchronized void put(int position, Page.Item item) {
            mImageSearchResults.put(position, item);
        }

        public synchronized void batchPut(int startPosition, Iterator<Page.Item> results) {
            while (results.hasNext()) {
                put(startPosition++, results.next());
            }
        }

        public synchronized Page.Item get(int position) {
            return mImageSearchResults.get(position);
        }

        public synchronized void clear() {
            mImageSearchResults.evictAll();
        }
    }

    public enum State {
        SET_SIZE_UNKNOWN,
        NON_EMPTY_SET,
        EMPTY_SET,
        ERROR
    }

    public interface StateChangeListener {
        public void onResultSetSizeChanged(int resultSetSize);
        public void onError(String error);
    }
}
