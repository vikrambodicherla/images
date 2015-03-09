package com.markiv.images.ui.search;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;

import com.markiv.gis.GISService;
import com.markiv.gis.Page;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class SearchPresenter implements SearchView.DataFetcher, SearchModel.StateChangeListener, SearchView.SearchViewStateChangeListener {
    private final Context mContext;
    private final String mQuery;

    private SearchView mSearchView;
    private SearchModel mSearchModel;

    private boolean mSearchFailed = false;

    public SearchPresenter(Context context, String query) {
        mContext = context;
        mQuery = query;
    }

    public void bind(SearchView searchView, SearchModel searchModel){
        mSearchView = searchView;
        mSearchModel = searchModel;

        mSearchModel.setStateChangeListener(this);
        mSearchView.setDataFetcher(this);
    }

    public SearchView.SearchViewStateChangeListener getSearchResultGetExceptionHandler(){
        return this;
    }

    /*
     * View related callbacks
     */
    @Override
    public void onGridReadyToBeShown() {
        if(!mSearchFailed){
            mSearchView.showGrid();
        }
    }

    @Override
    public void onSearchResultGetException(ExecutionException e) {
        mSearchModel.onDestroy();

        final String error = (e.getCause() instanceof GISService.SearchFailedException) ? e.getCause().getMessage() : mContext.getResources().getString(R.string.search_error);
        mSearchView.showError(error);

        mSearchFailed = true;
    }

    /*
     * Lifecycle callbacks
     */
    protected void onPause() {
        mSearchView.onPause();
        mSearchModel.onPause();
    }

    public void onLowMemory() {
        mSearchView.onLowMemory();
        mSearchModel.onLowMemory();
    }

    public void onDestroy() {
        mSearchView.onDestory();
        mSearchModel.onDestroy();
    }

    /*
     * Data-model related callbacks
     */

    @Override
    public void onResultSetSizeChanged(int resultSetSize) {
        if(mSearchFailed){
            return;
        }

        if(resultSetSize == 0){
            mSearchView.showMessage(mContext.getResources().getString(R.string.no_search_results, mQuery));
        }
        else {
            mSearchView.setDataChanged();
        }
    }

    @Override
    public void onResultsError(String error) {
        mSearchView.showError(error);
    }

    /*
     * Data fetcher interface
     */
    @Override
    public Future<Page.Item> getItem(int position) {
        return mSearchModel.fetchResult(position);
    }

    @Override
    public int getDataSetSize() {
        return mSearchModel.getResultCount();
    }
}
