
package com.markiv.images.ui;

import java.lang.ref.WeakReference;
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

import com.markiv.gis.GISService;
import com.markiv.gis.SearchSession;
import com.markiv.gis.image.GISImageView;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 1/25/15
 */
class GImageSearchAdapter extends BaseAdapter {
    private final Context mContext;
    private final SearchSession mSearchSession;
    private final GISService.GISImageViewFactory mImageViewFactory;

    private AbsListView.LayoutParams mCellLayoutParams;

    private final SearchActivity.ViewFlipperManager mViewSwitcherManager;

    public GImageSearchAdapter(Context context, SearchSession searchSession,
            GISService.GISImageViewFactory imageViewFactory,
            SearchActivity.ViewFlipperManager viewSwitcherManager) {
        mContext = context;
        mSearchSession = searchSession;
        mImageViewFactory = imageViewFactory;

        mViewSwitcherManager = viewSwitcherManager;

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

        ViewSetter viewSetter = (ViewSetter) networkImageView.getTag();
        if (viewSetter == null || viewSetter.getPosition() != position) {
            if (viewSetter != null) {
                viewSetter.cancelAndClearRefs();
            }

            viewSetter = new ViewSetter(networkImageView, position);
            networkImageView.setTag(viewSetter);

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
            cancel(true);
            mViewWeakReference.clear();
        }

        public int getPosition() {
            return mPosition;
        }

        @Override
        protected void onCancelled() {
            if (mResultFuture != null) {
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
                    Log.e("GImageSearchAdapter.ViewSetter", "Fetch interrupted", e);
                } catch (ExecutionException e) {
                    Log.e("GImageSearchAdapter.ViewSetter", "Fetch interrupted", e);
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

        public void setData(GISImageView view, SearchSession.Result data) {
            if (view != null) {
                if (data != null) {
                    view.setGISResult(data);

                    // TODO I'd want to do this when the first imageview is set, but for some reason
                    // that
                    // TODO doesnt work
                    mViewSwitcherManager.showGrid();
                } else if (mSearchSession.getResultCount() == 0) {
                    mViewSwitcherManager.showMessage(String.format(mContext.getResources()
                            .getString(R.string.no_search_results), mSearchSession.getQuery()));
                }
                else {
                    mViewSwitcherManager.showError(mError);
                }
            }
        }
    }

}
