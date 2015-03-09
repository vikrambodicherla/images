
package com.markiv.images.ui.search;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.markiv.gis.Page;
import com.markiv.gis.image.GISImageView;
import com.markiv.gis.image.ImageViewManager;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 1/25/15
 */
class GImageSearchAdapter extends BaseAdapter {
    private final Context mContext;
    private final ImageViewManager mImageViewFactory;
    private final AbsListView.LayoutParams mCellLayoutParams;

    //Do not manipulate this object on a non-UI thread. Access to this method if not thread-safe.
    private final HashMap<String, ViewSetter> mViewSetterHashMap = new HashMap<String, ViewSetter>();

    private final SearchView.DataFetcher mDataFetcher;

    private boolean mFirstImageLoaded = false;
    private final FirstImageLoadListener mFirstFirstImageLoadListener;
    private final SearchView.SearchViewStateChangeListener mSearchViewStateChangeListener;

    public GImageSearchAdapter(Context context, ImageViewManager imageViewFactory, SearchView.DataFetcher dataFetcher, FirstImageLoadListener firstFirstImageLoadListener, SearchView.SearchViewStateChangeListener searchViewStateChangeListener) {
        mContext = context;
        mImageViewFactory = imageViewFactory;
        mDataFetcher = dataFetcher;
        mFirstFirstImageLoadListener = firstFirstImageLoadListener;
        mSearchViewStateChangeListener = searchViewStateChangeListener;

        final Resources res = mContext.getResources();
        mCellLayoutParams = new AbsListView.LayoutParams((int)res.getDimension(R.dimen.grid_image_width), (int)res.getDimension(R.dimen.grid_image_height));
    }

    public void clear(){
        mViewSetterHashMap.clear();
    }

    @Override
    public int getCount() {
        return mDataFetcher.getDataSetSize();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GISImageView networkImageView;
        if (convertView == null) {
            networkImageView = mImageViewFactory.newImageView();
            networkImageView.setLayoutParams(mCellLayoutParams);
        }
        else {
            networkImageView = (GISImageView) convertView;
        }

        ViewSetter viewSetter = mViewSetterHashMap.get((String) networkImageView.getTag());
        if (viewSetter == null || viewSetter.getPosition() != position) {
            //Detach the viewsetter if necessary
            if (viewSetter != null) {
                viewSetter.cancelAndClearRefs();
                mViewSetterHashMap.remove(viewSetter.toString());
            }

            //Remove residual work from the viewsetter
            networkImageView.setImageDrawable(null);

            //Create a new viewsetter
            GISImageView.BitmapLoadedListener bitmapLoadedListener = null;
            if(!mFirstImageLoaded){
                bitmapLoadedListener = new GISImageView.BitmapLoadedListener() {
                    @Override
                    public void onBitmapLoaded() {
                        mFirstImageLoaded = true;
                        mFirstFirstImageLoadListener.onFirstImageLoaded();
                    }
                };
            };
            viewSetter = new ViewSetter(networkImageView, mDataFetcher.getItem(position), position, mSearchViewStateChangeListener, bitmapLoadedListener);
            final String viewSetterKey = viewSetter.toString();
            mViewSetterHashMap.put(viewSetterKey, viewSetter);
            networkImageView.setTag(viewSetterKey);

            viewSetter.execute((Void) null);
        }

        return networkImageView;
    }

    private static class ViewSetter extends AsyncTask<Void, Void, Page.Item> {
        private WeakReference<GISImageView> mViewWeakReference;
        private final Future<Page.Item> mResultFuture;
        private final int mPosition;

        private GISImageView.BitmapLoadedListener mListener;
        private SearchView.SearchViewStateChangeListener mSearchViewStateChangeListener;

        private ExecutionException mExecutionException;

        private ViewSetter(GISImageView view, Future<Page.Item> itemFuture, int position, SearchView.SearchViewStateChangeListener searchViewStateChangeListener, GISImageView.BitmapLoadedListener bitmapLoadedListener) {
            super();
            mViewWeakReference = new WeakReference<GISImageView>(view);
            mResultFuture = itemFuture;
            mPosition = position;
            mSearchViewStateChangeListener = searchViewStateChangeListener;
            mListener = bitmapLoadedListener;
        }

        public int getPosition() {
            return mPosition;
        }

        public void cancelAndClearRefs() {
            mViewWeakReference.clear();
            if(mResultFuture != null) {
                Log.d("ViewSetter", "Future cancelled");
                mResultFuture.cancel(true);
            }
            mSearchViewStateChangeListener = null;
            mListener = null;
            cancel(true);
        }

        @Override
        protected void onCancelled() {
            if (mResultFuture != null) {
                Log.d("ViewSetter", "Future cancelled2");
                mResultFuture.cancel(true);
            }
        }

        @Override
        protected Page.Item doInBackground(Void... params) {
            Thread.currentThread().setName("ViewSetter: " + mPosition);
            if (!isCancelled()) {
                try {
                    return mResultFuture.get();
                } catch (InterruptedException e) {
                    Log.d("ViewSetter", "Fetch interrupted", e);
                } catch (ExecutionException e){
                    mExecutionException = e;
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Page.Item item) {
            if (!isCancelled()) {
                setData(mViewWeakReference.get(), item);
            }
        }

        public void setData(final GISImageView view, Page.Item item) {
            if (view != null) {
                if (item != null) {
                    if(mListener != null){
                        view.setBitmapLoadedListener(mListener);
                    }
                    view.setGISResult(item);
                }
                else if(mExecutionException != null){
                    mSearchViewStateChangeListener.onSearchResultGetException(mExecutionException);
                }
                else {
                    //TODO One of the things I don't like!
                    Log.d("GImageSearchAdapter", "Null data");
                }
            }
        }
    }

    /**
     * Callback interface for receiving notification when the first image is loaded
     */
    public static interface FirstImageLoadListener {
        public void onFirstImageLoaded();
    }

}
