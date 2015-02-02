package com.markiv.images.ui;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
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

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.markiv.images.R;
import com.markiv.images.data.GISSession;
import com.markiv.images.data.VolleyProvider;
import com.markiv.images.data.model.GISResult;

/**
* @author vikrambd
* @since 1/25/15
*/
class GImageSearchAdapter extends BaseAdapter {
    private final Context mContext;
    private final GISSession mSearchSession;
    private final ImageLoader mImageLoader;

    private AbsListView.LayoutParams mCellLayoutParams;

    private final MainActivity.ViewSwitcherManager mViewSwitcherManager;

    private boolean mNonZeroResults = false;

    //TODO Externalize
    private static final int MAX_SEARCH_RESULTS = 64;

    public GImageSearchAdapter(Context context, GISSession searchSession, MainActivity.ViewSwitcherManager viewSwitcherManager) {
        mContext = context;
        mSearchSession = searchSession;
        mImageLoader = new ImageLoader(VolleyProvider.getInstance(context).getImageRequestQueue(), LruBitmapCache.getInstance(context));
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
        return MAX_SEARCH_RESULTS;
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
        NetworkImageView networkImageView;
        if(convertView == null){
            networkImageView = new NetworkImageView(mContext);
            networkImageView.setBackgroundColor(Color.RED);
            networkImageView.setLayoutParams(mCellLayoutParams);
            networkImageView.setAdjustViewBounds(true);
            networkImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        }
        else {
            networkImageView = (NetworkImageView) convertView;
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

    private class ViewSetter extends AsyncTask<Void, Void, GISResult> {
        private WeakReference<NetworkImageView> mViewWeakReference;
        private int mPosition;
        private Future<GISResult> mResultFuture;

        private ViewSetter(NetworkImageView view, int position) {
            mViewWeakReference = new WeakReference<NetworkImageView>(view);
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
        protected GISResult doInBackground(Void... params) {
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
        protected void onPostExecute(GISResult gisResult) {
            if(!isCancelled()) {
                setData(mViewWeakReference.get(), gisResult);
            }
        }

        public void setData(NetworkImageView view, GISResult data){
            if(data != null) {
                mViewSwitcherManager.showGrid();
                view.setImageUrl(data.getTbUrl(), mImageLoader);
            }
            else {
                //TODO Have a second grayed line with the actual error description.
                mViewSwitcherManager.displayMessage(R.string.search_error);
            }
        }
    }
}
