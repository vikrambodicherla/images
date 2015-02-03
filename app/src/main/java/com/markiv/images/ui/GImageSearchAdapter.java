package com.markiv.images.ui;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
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
import com.markiv.images.BuildConfig;
import com.markiv.images.R;
import com.markiv.images.data.GISSession;
import com.markiv.images.data.VolleyProvider;
import com.markiv.images.data.model.GISResult;

/**
* @author vikrambd
* @since 1/25/15
*/
class GImageSearchAdapter extends BaseAdapter {
    private static final int MAX_SEARCH_RESULTS = BuildConfig.MAX_SEARCH_RESULTS;

    private final Context mContext;
    private final GISSession mSearchSession;
    private final ImageLoader mImageLoader;

    private AbsListView.LayoutParams mCellLayoutParams;

    private final SearchActivity.ViewFlipperManager mViewSwitcherManager;

    /*
     * We start by assuming we have 64 results. The first time we get a response, we adjust this number
     * if needed
     */
    private boolean resultCountAdjusted = false;
    private int actualResultCount = -1;
    private int displayedResultCount = MAX_SEARCH_RESULTS;

    public GImageSearchAdapter(Context context, GISSession searchSession, SearchActivity.ViewFlipperManager viewSwitcherManager) {
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
        NetworkImageView networkImageView;
        if(convertView == null){
            networkImageView = new FadeInNetworkImageView(mContext);
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
            if(view != null) {
                if (data != null) {
                    view.setImageUrl(data.getTbUrl(), mImageLoader);

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

    public class FadeInNetworkImageView extends NetworkImageView {
        private static final int FADE_IN_TIME_MS = 250;

        public FadeInNetworkImageView(Context context) {
            super(context);
        }

        public FadeInNetworkImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void setImageBitmap(Bitmap bm) {
            //For the very first loaded bitmap, we turn off the progressbar.
            //This is a no-op subsequently (the ViewFlipper takes care of this)
            //TODO For some reason, this wont work! So I am dismissing the progress bar earlier
            //mViewSwitcherManager.showGrid();

            TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                    new ColorDrawable(android.R.color.transparent),
                    new BitmapDrawable(getContext().getResources(), bm)
            });

            setImageDrawable(td);
            td.startTransition(FADE_IN_TIME_MS);
        }
    }
}
