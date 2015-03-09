
package com.markiv.images.ui;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.markiv.gis.GISService;
import com.markiv.gis.Page;
import com.markiv.gis.util.CurrentFuture;
import com.markiv.gis.util.WrappedFuture;

/**
 * The SearchModel uses the GIS API (via the GISService) to fetch pages. It maintains an LRU-cache of
 * list items. Changes to the state of data can be listened to via the StateChangeListener
 * @author vikrambd
 * @since 3/7/15
 */
public class SearchModel {
    private final GISService mGISService;

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
     * @param gisService
     */
    SearchModel(GISService gisService) {
        mGISService = gisService;
        mCache = new ResultsCache();

        //TODO: We need a better strategy here
        mPageSize = mGISService.getMaxPageSize();

        //Fetch count. While we can optimize the hell out of this by getting this info via the first call
        //this approach is cleaner and easier to test
        new AsyncTask<Void, Void, Integer>(){
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mGISService.fetchResultSetSize().get();
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
                    mState = mDisplayedResultCount == 0 ? State.EMPTY_SET : State.NON_EMPTY_SET;
                    if(mStateChangeListener != null){
                        mStateChangeListener.onResultSetSizeChanged(mDisplayedResultCount);
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
        mGISService.cancelAll();
    }

    public void onDestroy(){
        mGISService.stop();
        mCache.clear();
        mStateChangeListener = null;
    }

    public void onLowMemory(){
        mGISService.cancelAll();
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
                return new WrappedFuture<Page.Item, Page>(mGISService.fetchPage(
                        getPageNumber(position), mPageSize),
                        new WrappedFuture.Adapter<Page.Item, Page>() {
                            @Override
                            public Page.Item convert(Page result) throws ExecutionException {
                                try {
                                    processResponse(result);
                                }
                                catch (final GISService.SearchFailedException e){
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
            } catch (GISService.UnsupportedSearchRequestException e) {
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

    /*
     * Process the page and cache the response.
     */
    private void processResponse(Page page) throws GISService.SearchFailedException {
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
