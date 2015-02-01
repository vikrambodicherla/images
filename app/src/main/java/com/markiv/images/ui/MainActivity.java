package com.markiv.images.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.GridView;

import com.markiv.images.R;
import com.markiv.images.data.GISSession;

public class MainActivity extends ActionBarActivity {
    private GridView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScrollView = (GridView) findViewById(R.id.scrollview);

        GISSession gisSession = GISSession.newSession("uber", 4);
        GImageSearchAdapter adapter = new GImageSearchAdapter(this, gisSession);
        mScrollView.setAdapter(adapter);
    }

    private void toast(int position, String title){
        Log.d("ImagesBlah", position + ": " + title);
    }
}
