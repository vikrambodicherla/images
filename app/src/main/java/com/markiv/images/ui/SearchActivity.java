package com.markiv.images.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.markiv.gis.GISImageManager;
import com.markiv.gis.GISService;
import com.markiv.gis.SearchSession;
import com.markiv.images.R;
import com.markiv.images.ui.history.SearchHistoryManager;

public class SearchActivity extends ActionBarActivity {
    private static final String QUERY = "query";
    private ViewFlipperManager mViewSwitcherManager;

    private GISService mGISService;
    private SearchSession mActiveSession;
    private GISImageManager mImageManager;

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQuery = (savedInstanceState != null) ? savedInstanceState.getString(QUERY) : getQueryFromIntent(getIntent());
        if(mQuery == null){
            finish();
        }

        setContentView(R.layout.activity_search);
        
        mViewSwitcherManager = new ViewFlipperManager();
        mGISService = new GISService(this, 8);
        mImageManager = mGISService.newImageManager();

        // Setup the UI
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mQuery);
        actionBar.setDisplayHomeAsUpEnabled(true);

        search(mQuery);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(QUERY, mActiveSession.getQuery());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        mImageManager.cancelAll();
        mActiveSession.cancelAll();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mActiveSession.stop();
        mImageManager.stop();

        super.onDestroy();
    }

    private String getQueryFromIntent(Intent intent){
        String query = null;
        if(intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = getIntent().getStringExtra(SearchManager.QUERY);
        }

        return !TextUtils.isEmpty(query) ? query : null;
    }

    private void search(String query){
        new SearchHistoryManager(this).recordSearch(query);
        if(mActiveSession == null || !query.equals(mActiveSession.getQuery())){
            if(mActiveSession != null){
                mActiveSession.stop();
            }

            //TODO Optimize, make smaller pages on a smaller device - less memory or smaller screen size
            mActiveSession = mGISService.startSearch(query);
            mViewSwitcherManager.setGridAdapter(new GImageSearchAdapter(this, mActiveSession, mImageManager, mViewSwitcherManager));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_search, menu);

        final MenuItem searchItem = menu.findItem(R.id.ic_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    class ViewFlipperManager {
        private ViewSwitcher mViewFlipper;

        private GridView mScrollView;
        private TextView mMessagesTextView;
        private TextView mErrorMessageTextView;
        private ProgressBar mProgressBar;

        ViewFlipperManager() {
            mViewFlipper = (ViewSwitcher) findViewById(R.id.search_switcher);

            mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
            mScrollView = (GridView) findViewById(R.id.search_grid);
            mMessagesTextView = (TextView) findViewById(R.id.search_message);
            mErrorMessageTextView = (TextView) findViewById(R.id.search_error_detail);
        }

        public void setGridAdapter(ListAdapter adapter){
            mScrollView.setAdapter(adapter);
        }

        public void showGrid(){
            mProgressBar.setVisibility(View.GONE);
            mViewFlipper.setDisplayedChild(0);
        }

        public void showError(String message){
            mProgressBar.setVisibility(View.GONE);
            mViewFlipper.setDisplayedChild(1);
            mMessagesTextView.setText(R.string.search_error);
            if(!TextUtils.isEmpty(message)) {
                mErrorMessageTextView.setVisibility(View.VISIBLE);
                mErrorMessageTextView.setText(message);
            }
            else {
                mErrorMessageTextView.setVisibility(View.INVISIBLE);
            }
        }

        public void showMessage(int stringResId){
            mProgressBar.setVisibility(View.GONE);
            mViewFlipper.setDisplayedChild(1);
            mMessagesTextView.setText(stringResId);
            mErrorMessageTextView.setVisibility(View.INVISIBLE);
        }

        public void showMessage(String message){
            mProgressBar.setVisibility(View.GONE);
            mViewFlipper.setDisplayedChild(1);
            mMessagesTextView.setText(message);
            mErrorMessageTextView.setVisibility(View.INVISIBLE);
        }
    }
}