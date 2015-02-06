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
import android.widget.ImageView;

import com.markiv.gis.GISService;
import com.markiv.gis.image.GISImageView;
import com.markiv.images.R;

/**
* @author vikrambd
* @since 1/25/15
*/
class GImageSearchAdapter extends BaseAdapter {
    private static final int MAX_SEARCH_RESULTS = 64;

    private final Context mContext;
    private final GISService.Session mSearchSession;
    private final GISService.GISImageViewFactory mImageViewFactory;

    private AbsListView.LayoutParams mCellLayoutParams;

    private final SearchActivity.ViewFlipperManager mViewSwitcherManager;

    /*
     * We start by assuming we have 64 results. The first time we get a response, we adjust this number
     * if needed
     */
    private boolean resultCountAdjusted = false;
    private int actualResultCount = -1;
    private int displayedResultCount = MAX_SEARCH_RESULTS;

    public GImageSearchAdapter(Context context, GISService.Session searchSession, GISService.GISImageViewFactory imageViewFactory, SearchActivity.ViewFlipperManager viewSwitcherManager) {
        mContext = context;
        mSearchSession = searchSession;
        mImageViewFactory = imageViewFactory;

        mViewSwitcherManager = viewSwitcherManager;

        setupCellLayoutParams();
    }

    private void setupCellLayoutParams(){
        final Resources res = mContext.getResources();
        final int gridHorizontalSpacing = res.getDimensionPixelSize(R.dimen.grid_horizontal_spacing);

        final Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final int screenWidth = size.x;

        final int cellWidth = (screenWidth - 4 * gridHorizontalSpacing)/3;
        mCellLayoutParams = new AbsListView.LayoutParams(cellWidth, GridView.AUTO_FIT);
    }

    @Override
    public int getCount() {
        return displayedResultCount;
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
        if(convertView == null){
            networkImageView = mImageViewFactory.newImageView();
            networkImageView.setLayoutParams(mCellLayoutParams);
        }
        else {
            networkImageView = (GISImageView) convertView;
        }

        ViewSetter viewSetter = (ViewSetter) networkImageView.getTag();
        if(viewSetter == null || viewSetter.getPosition() != position){
            if(viewSetter != null){
                viewSetter.cancelAndClearRefs();
            }

            viewSetter = new ViewSetter(networkImageView, position);
            networkImageView.setTag(viewSetter);

            viewSetter.execute((Void)null);
        }

        return networkImageView;
    }

    private class ViewSetter extends AsyncTask<Void, Void, GISService.Result> {
        private WeakReference<GISImageView> mViewWeakReference;
        private int mPosition;
        private Future<GISService.Result> mResultFuture;

        private ViewSetter(GISImageView view, int position) {
            mViewWeakReference = new WeakReference<GISImageView>(view);
            mPosition = position;
        }

        public void cancelAndClearRefs(){
            cancel(true);
            mViewWeakReference.clear();
        }

        public int getPosition() {
            return mPosition;
        }

        @Override
        protected void onCancelled() {
            if(mResultFuture != null){
                mResultFuture.cancel(true);
            }
        }

        @Override
        protected GISService.Result doInBackground(Void... params) {
            if(!isCancelled()) {
                try {
                    //TODO We should ideally be asking for images of the required size, but the GISService doesn't provide for this
                    mResultFuture = mSearchSession.fetchResult(mPosition);
                    return mResultFuture.get();
                } catch (InterruptedException e) {
                    Log.e("GImageSearchAdapter.ViewSetter", "Fetch interrupted", e);

                } catch (ExecutionException e) {
                    Log.e("GImageSearchAdapter.ViewSetter", "Fetch interrupted", e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(GISService.Result gisResult) {
            if(!isCancelled()) {
                setData(mViewWeakReference.get(), gisResult);
            }
        }

        public void setData(GISImageView view, GISService.Result data){
            if(view != null) {
                if (data != null) {
                    view.setGISResult(data);

                    //TODO I'd want to do this when the first imageview is set, but for some reason that
                    //TODO doesnt work
                    mViewSwitcherManager.showGrid();

                    if (!resultCountAdjusted) {
                        adjustResultCount();
                    }

                } else {
                    if (mSearchSession.getResultCount() == 0) {
                        mViewSwitcherManager.showMessage(String.format(mContext.getResources().getString(R.string.no_search_results), mSearchSession.getQuery()));
                    } else {
                        //TODO Have a second grayed line with the actual error description.
                        mViewSwitcherManager.showMessage(R.string.search_error);
                    }
                }
            }
        }
    }

    private void adjustResultCount(){
        resultCountAdjusted = true;
        actualResultCount = mSearchSession.getResultCount();

        if(actualResultCount < MAX_SEARCH_RESULTS){
            //If we have more than 64 results, let's just show 64 results because that's the max that
            //Google Search allows
            displayedResultCount = actualResultCount;
            notifyDataSetChanged();
        }
    }
}
