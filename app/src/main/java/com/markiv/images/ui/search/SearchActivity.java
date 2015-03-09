
package com.markiv.images.ui.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import com.markiv.gis.GISService;
import com.markiv.gis.image.ImageViewManager;
import com.markiv.images.BuildConfig;
import com.markiv.images.R;
import com.markiv.images.ui.history.SearchHistoryManager;

public class SearchActivity extends ActionBarActivity {
    private static final String QUERY = "query";
    private SearchPresenter mSearchPresenter;

    private ImageViewManager mImageViewManager;

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strictmode for debug
        setupStrictModeIfDebug();

        if(!extractQuery(savedInstanceState)){
            finish();
            return;
        }

        // Setup the UI
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mQuery);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_search);

        //Record search
        new SearchHistoryManager(this).recordSearch(mQuery);

        //M, V and P
        mImageViewManager = ImageViewManager.newInstance(this);

        mSearchPresenter = new SearchPresenter(this, mQuery);
        SearchView searchView = new SearchView(this, mImageViewManager, mSearchPresenter.getSearchResultGetExceptionHandler());
        SearchModel searchModel = new SearchModel(GISService.newInstance(this, mQuery));

        mSearchPresenter.bind(searchView, searchModel);
    }

    private void setupStrictModeIfDebug(){
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
                    .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
                    .build());
        }
    }

    private boolean extractQuery(Bundle savedInstanceState){
        mQuery = (savedInstanceState != null) ? savedInstanceState.getString(QUERY)
                : getQueryFromIntent(getIntent());
        return !TextUtils.isEmpty(mQuery);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(QUERY, mQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSearchPresenter.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mSearchPresenter.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSearchPresenter.onDestroy();
    }

    private String getQueryFromIntent(Intent intent) {
        String query = null;
        if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
        }

        return !TextUtils.isEmpty(query) ? query : null;

    }
}
