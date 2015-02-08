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
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.markiv.gis.GISService;
import com.markiv.gis.SearchSession;
import com.markiv.images.R;
import com.markiv.images.ui.history.SearchHistoryManager;

public class SearchActivity extends ActionBarActivity {
    private static final String QUERY = "query";
    private ViewFlipperManager mViewSwitcherManager;

    private GISService mGISService;
    private SearchSession mActiveSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mViewSwitcherManager = new ViewFlipperManager();
        mGISService = new GISService(this, 8);

        final String query = (savedInstanceState != null) ? savedInstanceState.getString(QUERY) : getQueryFromIntent(getIntent());
        if(query != null){
            //Setup the UI
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(query);
            actionBar.setDisplayHomeAsUpEnabled(true);

            //Search
            search(query);
        }
        else {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(QUERY, mActiveSession.getQuery());
        super.onSaveInstanceState(outState);
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
                mActiveSession.kill();
            }

            //TODO Optimize, make smaller pages on a smaller device - less memory or smaller screen size
            mActiveSession = mGISService.startSearch(query);
            mViewSwitcherManager.setGridAdapter(new GImageSearchAdapter(this, mActiveSession, mGISService.getImageViewFactory(), mViewSwitcherManager));
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
        private GridView mScrollView;
        private TextView mMessagesTextView;
        private TextView mErrorMessageTextView;

        private ViewFlipper mViewFlipper;

        ViewFlipperManager() {
            mViewFlipper = (ViewFlipper) findViewById(R.id.search_switcher);

            mScrollView = (GridView) findViewById(R.id.search_grid);
            mMessagesTextView = (TextView) findViewById(R.id.search_message);
            mErrorMessageTextView = (TextView) findViewById(R.id.search_error_detail);
        }

        public void setGridAdapter(ListAdapter adapter){
            mScrollView.setAdapter(adapter);
        }

        public void showGrid(){
            mViewFlipper.setDisplayedChild(2);
        }

        public void showError(String message){
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
            mViewFlipper.setDisplayedChild(1);
            mMessagesTextView.setText(stringResId);
            mErrorMessageTextView.setVisibility(View.INVISIBLE);
        }

        public void showMessage(String message){
            mViewFlipper.setDisplayedChild(1);
            mMessagesTextView.setText(message);
            mErrorMessageTextView.setVisibility(View.INVISIBLE);
        }
    }
}