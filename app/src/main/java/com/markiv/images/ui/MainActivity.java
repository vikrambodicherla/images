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
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.markiv.images.R;
import com.markiv.images.data.GISSession;

public class MainActivity extends ActionBarActivity {
    private ViewSwitcher mViewSwitcher;
    private GridView mScrollView;
    private TextView mMessagesTextView;
    private ViewSwitcherManager mViewSwitcherManager;

    private GISSession mGISSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.main_switcher);
        mScrollView = (GridView) findViewById(R.id.main_grid);
        mMessagesTextView = (TextView) findViewById(R.id.main_message);
        mViewSwitcherManager = new ViewSwitcherManager();

        handleSearchIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSearchIntent(intent);
    }

    private void handleSearchIntent(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if(TextUtils.isEmpty(query)){
                return;
            }

            if(mGISSession == null || !query.equals(mGISSession.getQuery())){
                if(mGISSession != null){
                    mGISSession.persist();
                    mGISSession.kill();
                }

                setupNewSession(query);
            }

            final ActionBar actionBar = getSupportActionBar();
            actionBar.collapseActionView();
            actionBar.setTitle(query);

            //Preemptively set this with a delay, let the adapter unset it
            mViewSwitcher.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mViewSwitcherManager.displayMessage(R.string.no_search_results);
                }
            }, 1000);
        }
    }

    private void setupNewSession(String query){
        //TODO Optimize, make smaller pages on a smaller device - less memory or smaller screen size
        mGISSession = GISSession.newSession(query, 8);
        GImageSearchAdapter adapter = new GImageSearchAdapter(this, mGISSession, mViewSwitcherManager);
        mScrollView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        MenuItem searchItem = menu.findItem(R.id.main_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        return true;
    }

    class ViewSwitcherManager {
        public void showGrid(){
            mViewSwitcher.setDisplayedChild(1);
        }

        public void displayMessage(int messageResId){
            mViewSwitcher.setDisplayedChild(0);
            mMessagesTextView.setText(messageResId);
        }
    }
}
