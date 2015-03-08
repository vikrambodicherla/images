
package com.markiv.images.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import com.markiv.gis.GISClient;
import com.markiv.gis.image.ImageViewManager;
import com.markiv.images.BuildConfig;
import com.markiv.images.R;

public class SearchActivity extends ActionBarActivity {
    private static final String QUERY = "query";
    private SearchModel mSearchModel;
    private SearchView mSearchView;
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

        //M, V and P
        mImageViewManager = ImageViewManager.newInstance(this);

        mSearchView = new SearchView(this, mImageViewManager);
        mSearchModel = new SearchModel(GISClient.newInstance(this, mQuery));
        mSearchPresenter = new SearchPresenter(mSearchView, mSearchModel);
        mSearchPresenter.bind();
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
        mSearchView.onPause();
        mSearchModel.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mSearchView.onLowMemory();
        mSearchModel.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSearchView.onDestory();
        mSearchModel.onDestroy();
    }

    private String getQueryFromIntent(Intent intent) {
        String query = null;
        if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
        }

        return !TextUtils.isEmpty(query) ? query : null;

    }
}
