package com.markiv.images.ui;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import com.markiv.images.GImageSearchApplication;
import com.markiv.images.R;
import com.markiv.images.data.GISSession;

public class MainActivity extends ActionBarActivity {
    private GridView mScrollView;

    private boolean mRegisteredForErrors = false;
    private ErrorBroadcastManager mErrorBroadcastManager;
    private IntentFilter mErrorIntentFilter;

    private GISSession mGISSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScrollView = (GridView) findViewById(R.id.scrollview);

        mErrorBroadcastManager = new ErrorBroadcastManager();
        mErrorIntentFilter = new IntentFilter(GImageSearchApplication.ACTION_GSAPI_ERROR);

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
        }
    }

    private void setupNewSession(String query){
        //TODO Optimize, make smaller pages on a smaller device - less memory or smaller screen size
        mGISSession = GISSession.newSession(query, 8);
        GImageSearchAdapter adapter = new GImageSearchAdapter(this, mGISSession);
        mScrollView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mRegisteredForErrors){
            registerReceiver(mErrorBroadcastManager, mErrorIntentFilter);
            mRegisteredForErrors = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        MenuItem searchItem = menu.findItem(R.id.main_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRegisteredForErrors){
            unregisterReceiver(mErrorBroadcastManager);
            mRegisteredForErrors = false;
        }
    }

    private void handleServerError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class ErrorBroadcastManager extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleServerError("Unable to connect to the server");
        }
    }
}
