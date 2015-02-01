package com.markiv.images.ui;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
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

    //TODO Externalize
    private static final int MAX_SEARCH_RESULTS = 64;

    public GImageSearchAdapter(Context context, GISSession searchSession) {
        mContext = context;
        mSearchSession = searchSession;
        mImageLoader = new ImageLoader(VolleyProvider.getInstance(context).getImageRequestQueue(), LruBitmapCache.getInstance(context));
    }

    @Override
    public int getCount() {
        return MAX_SEARCH_RESULTS;
    }

    @Override
    public Object getItem(int position) {
        //return mSearchSession.blockingGetSearchResult2(position);
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
            Log.d("GImageSearchAdapter", mPosition + ": " + data.getTitle());
            view.setImageUrl(data.getUrl(), mImageLoader);
        }
    }
}
