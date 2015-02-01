package com.markiv.images.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScrollView = (GridView) findViewById(R.id.scrollview);

        mErrorBroadcastManager = new ErrorBroadcastManager();
        mErrorIntentFilter = new IntentFilter(GImageSearchApplication.ACTION_GSAPI_ERROR);

        GISSession gisSession = GISSession.newSession("rafa", 8);
        GImageSearchAdapter adapter = new GImageSearchAdapter(this, gisSession);
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

        }
    }
}
