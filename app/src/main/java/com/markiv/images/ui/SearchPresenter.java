package com.markiv.images.ui;

import java.util.concurrent.Future;

import com.markiv.gis.Page;
import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class SearchPresenter implements SearchView.DataFetcher, SearchModel.StateChangeListener {
    private final SearchView mSearchView;
    private final SearchModel mSearchModel;

    public SearchPresenter(SearchView searchView, SearchModel searchModel) {
        mSearchView = searchView;
        mSearchModel = searchModel;
    }

    public void bind(){
        mSearchModel.setStateChangeListener(this);
        mSearchView.setDataFetcher(this);
    }

    @Override
    public void onResultSetSizeChanged(int resultSetSize) {
        if(resultSetSize == 0){
            mSearchView.showMessage(R.string.no_search_results);
        }
        else {
            mSearchView.setDataChanged();
        }
    }

    @Override
    public void onError(String error) {
        mSearchView.showError(error);
    }

    @Override
    public Future<Page.Item> getItem(int position) {
        return mSearchModel.fetchResult(position);
    }

    @Override
    public int getDataSetSize() {
        return mSearchModel.getResultCount();
    }
}
