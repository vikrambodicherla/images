
package com.markiv.images.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.markiv.gis.SearchSession;
import com.markiv.gis.image.GISImageView;
import com.markiv.gis.image.ImageViewManager;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 1/25/15
 */
class GImageSearchAdapter extends BaseAdapter {
    private final Context mContext;
    private final SearchSession mSearchSession;
    private final ImageViewManager mImageViewFactory;

    private AbsListView.LayoutParams mCellLayoutParams;

    private final OnSearchStateChangeListener mSearchStateChangeListener;

    private boolean mFirstImageLoaded = false;

    //Do not manipulate this object on a non-UI thread. Access to this method if not thread-safe.
    private final HashMap<String, ViewSetter> mViewSetterHashMap = new HashMap<String, ViewSetter>();

    public GImageSearchAdapter(Context context, SearchSession searchSession,
            ImageViewManager imageViewFactory,
            OnSearchStateChangeListener searchStateChangeListener) {
        mContext = context;
        mSearchSession = searchSession;
        mImageViewFactory = imageViewFactory;
        mSearchStateChangeListener = searchStateChangeListener;

        setupCellLayoutParams();

        mSearchSession
                .setSearchResultSetUpdatesListener(new SearchSession.SearchResultSetUpdateListener() {
                    @Override
                    public void onResultSetSizeChanged() {
                        notifyDataSetChanged();
                    }
                });
    }

    private void setupCellLayoutParams() {
        final Resources res = mContext.getResources();
        final int gridHorizontalSpacing = res
                .getDimensionPixelSize(R.dimen.grid_horizontal_spacing);

        final Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final int screenWidth = size.x;

        final int cellWidth = (screenWidth - 4 * gridHorizontalSpacing) / 3;
        mCellLayoutParams = new AbsListView.LayoutParams(cellWidth, GridView.AUTO_FIT);
    }

    public void clear(){
        mViewSetterHashMap.clear();
    }

    @Override
    public int getCount() {
        return mSearchSession.getResultCount();
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
            if (viewSetter != null) {
                viewSetter.cancelAndClearRefs();
                mViewSetterHashMap.remove(viewSetter.toString());
            }

            viewSetter = new ViewSetter(networkImageView, position);
            final String viewSetterKey = viewSetter.toString();
            mViewSetterHashMap.put(viewSetterKey, viewSetter);
            networkImageView.setTag(viewSetterKey);

            viewSetter.execute((Void) null);
        }

        return networkImageView;
    }

    private class ViewSetter extends AsyncTask<Void, Void, SearchSession.Result> {
        private WeakReference<GISImageView> mViewWeakReference;

        private int mPosition;
        private Future<SearchSession.Result> mResultFuture;

        private String mError = null;

        private ViewSetter(GISImageView view, int position) {
            mViewWeakReference = new WeakReference<GISImageView>(view);
            mPosition = position;
        }

        public void cancelAndClearRefs() {
            mViewWeakReference.clear();
            if(mResultFuture != null) {
                Log.d("ViewSetter", "Future cancelled");
                mResultFuture.cancel(true);
            }
            cancel(true);
        }

        public int getPosition() {
            return mPosition;
        }

        @Override
        protected void onCancelled() {
            if (mResultFuture != null) {
                Log.d("ViewSetter", "Future cancelled2");
                mResultFuture.cancel(true);
            }
        }

        @Override
        protected SearchSession.Result doInBackground(Void... params) {
            if (!isCancelled()) {
                try {
                    mResultFuture = mSearchSession.fetchResult(mPosition);
                    return mResultFuture.get();
                } catch (InterruptedException e) {
                    Log.e("ViewSetter", "Fetch interrupted", e);
                } catch (ExecutionException e) {
                    Log.e("ViewSetter", "Fetch failed", e);
                    if (e.getCause() instanceof SearchSession.SearchFailedException) {
                        mError = e.getCause().getMessage();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(SearchSession.Result gisResult) {
            if (!isCancelled()) {
                setData(mViewWeakReference.get(), gisResult);
            }
        }

        public void setData(final GISImageView view, SearchSession.Result data) {
            if (view != null) {
                if (data != null) {
                    if (!mFirstImageLoaded) {
                        view.setBitmapLoadedListener(new GISImageView.BitmapLoadedListener() {
                            @Override
                            public void onBitmapLoaded() {
                                mFirstImageLoaded = true;
                                mSearchStateChangeListener.onAdapterReady();
                            }
                        });
                    }
                    view.setGISResult(data);
                } else if (mSearchSession.getResultCount() == 0) {
                    mSearchStateChangeListener.onZeroResults();
                }
                else {
                    mSearchSession.cancelAll();
                    mSearchStateChangeListener.onSearchError(mError);
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + "_" + mPosition;
        }
    }

    public interface OnSearchStateChangeListener {
        public void onAdapterReady();
        public void onZeroResults();
        public void onSearchError(String error);
    }
}
