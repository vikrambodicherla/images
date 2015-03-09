package com.markiv.images.ui;

import java.util.concurrent.Future;

import android.text.TextUtils;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.markiv.gis.Page;
import com.markiv.gis.image.ImageViewManager;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class SearchView {
    private final ViewSwitcher mViewFlipper;

    private final GridView mGridView;
    private final TextView mMessagesTextView;
    private final TextView mErrorMessageTextView;
    private final ProgressBar mProgressBar;

    private final ImageViewManager mImageViewManager;

    private GImageSearchAdapter mSearchAdapter;

    public SearchView(SearchActivity searchActivity, ImageViewManager imageViewManager){
        mViewFlipper = (ViewSwitcher) searchActivity.findViewById(R.id.search_switcher);

        mProgressBar = (ProgressBar) searchActivity.findViewById(R.id.search_progress);
        mGridView = (GridView) searchActivity.findViewById(R.id.search_grid);
        mGridView.setVerticalScrollBarEnabled(false);
        mMessagesTextView = (TextView) searchActivity.findViewById(R.id.search_message);
        mErrorMessageTextView = (TextView) searchActivity.findViewById(R.id.search_error_detail);

        mImageViewManager = imageViewManager;
    }

    public void setDataFetcher(DataFetcher dataFetcher){
        mSearchAdapter = new GImageSearchAdapter(mViewFlipper.getContext(), mImageViewManager, dataFetcher, new GImageSearchAdapter.FirstImageLoadListener() {
            @Override
            public void onFirstImageLoaded() {
                showGrid();
            }
        });
        mGridView.setAdapter(mSearchAdapter);
    }

    public void onPause(){
        mImageViewManager.cancelAll();
    }

    public void onDestory(){
        mSearchAdapter.clear();
        mImageViewManager.stop();
    }

    public void onLowMemory(){
        mImageViewManager.cleanUp();
    }

    public void setDataChanged(){
        mSearchAdapter.notifyDataSetChanged();
    }

    public void showGrid() {
        mProgressBar.setVisibility(View.GONE);
        mViewFlipper.setDisplayedChild(0);
        mGridView.setVerticalScrollBarEnabled(true);
    }

    public void showError(String message) {
        mProgressBar.setVisibility(View.GONE);
        mViewFlipper.setDisplayedChild(1);
        mMessagesTextView.setText(R.string.search_error);
        if (!TextUtils.isEmpty(message)) {
            mErrorMessageTextView.setVisibility(View.VISIBLE);
            mErrorMessageTextView.setText(message);
        }
        else {
            mErrorMessageTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void showMessage(String message) {
        mProgressBar.setVisibility(View.GONE);
        mViewFlipper.setDisplayedChild(1);
        mMessagesTextView.setText(message);
        mErrorMessageTextView.setVisibility(View.INVISIBLE);
    }

    public static interface DataFetcher {
        public Future<Page.Item> getItem(int position);
        public int getDataSetSize();
    }
}
