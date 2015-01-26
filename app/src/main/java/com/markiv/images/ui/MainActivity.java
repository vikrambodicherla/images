
package com.markiv.images.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.markiv.images.R;
import com.markiv.images.data.GISearchService;
import com.markiv.images.data.GImageSearchSession;
import com.markiv.images.data.model.GISearchResult;

public class MainActivity extends ActionBarActivity {
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GImageSearchSession searchSession = GISearchService.getInstance(this)
                .newSearchSession("uber").withPageSize(4).start();
        //searchSession.blockingGetSearchResult(0);
        searchSession.getSearchResult(0, new Response.Listener<GISearchResult>() {
            @Override
            public void onResponse(GISearchResult response) {
                Toast.makeText(MainActivity.this, response.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        //((TextView)findViewById(R.id.view1)).setText(searchSession.blockingGetSearchResult(0).getTitle());
        //((TextView)findViewById(R.id.view2)).setText(searchSession.blockingGetSearchResult(1).getTitle());
        //((TextView)findViewById(R.id.view3)).setText(searchSession.blockingGetSearchResult(2).getTitle());
        //((TextView)findViewById(R.id.view4)).setText(searchSession.blockingGetSearchResult(3).getTitle());
        //mListView = (ListView) findViewById(R.id.image_list);
        //mListView.setAdapter(new Adapter(this, );


    }

}
