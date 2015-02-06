package com.markiv.gis;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;

import com.android.volley.toolbox.ImageLoader;
import com.markiv.gis.api.SearchSession;
import com.markiv.gis.api.VolleyProvider;
import com.markiv.gis.api.model.APIResult;
import com.markiv.gis.image.GISImageView;
import com.markiv.gis.image.LruBitmapCache;

/**
 * @author vikrambd
 * @since 2/5/15
 */
public class GISService {
    private Context mContext;
    private int mPageSize;
    private GISImageViewFactory mImageViewFactory;

    public GISService(Context context, int pageSize) {
        mContext = context;
        mPageSize = pageSize;
        mImageViewFactory = new GISImageViewFactory();
    }

    public Session startSearch(String query){
        return new Session(SearchSession.newSession(mContext, query, mPageSize));
    }

    public GISImageViewFactory getImageViewFactory(){
        return mImageViewFactory;
    }

    public class Session {
        private final SearchSession mSearchSession;

        private Session(SearchSession searchSession){
            mSearchSession = searchSession;
        }

        public Future<Result> fetchResult(int position){
            final Future<APIResult> apiResultFuture = mSearchSession.fetchResult(position);
            return new Future<Result>(){
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return apiResultFuture.cancel(mayInterruptIfRunning);
                }

                @Override
                public boolean isCancelled() {
                    return apiResultFuture.isCancelled();
                }

                @Override
                public boolean isDone() {
                    return apiResultFuture.isDone();
                }

                @Override
                public Result get() throws InterruptedException, ExecutionException {
                    return new Result(apiResultFuture.get());
                }

                @Override
                public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return new Result(apiResultFuture.get(timeout, unit));
                }
            };
        }

        public int getResultCount(){
            return mSearchSession.getResultCount();
        }

        public String getQuery(){
            return mSearchSession.getQuery();
        }

        public void kill(){
            mSearchSession.kill();
        }
    }

    public class Result {
        private final APIResult mAPIResult;

        private Result(APIResult apiResult){
            mAPIResult = apiResult;
        }

        public String getTitle(){
            return mAPIResult.getTitleNoFormatting();
        }

        public String getUrl(){
            return mAPIResult.getTbUrl();
        }
    }

    public class GISImageViewFactory {
        private ImageLoader mImageLoader;

        public GISImageViewFactory() {
            mImageLoader = new ImageLoader(VolleyProvider.getInstance(mContext).getImageRequestQueue(), LruBitmapCache.getInstance(mContext));
        }

        public GISImageView newImageView(){
            return new GISImageView(mContext, mImageLoader);
        }
    }
}
