
package com.markiv.images.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

        ((TextView)findViewById(R.id.view1)).setText(searchSession.blockingGetSearchResult(0).getTitle());
        //((TextView)findViewById(R.id.view2)).setText(searchSession.blockingGetSearchResult(1).getTitle());
        //((TextView)findViewById(R.id.view3)).setText(searchSession.blockingGetSearchResult(2).getTitle());
        //((TextView)findViewById(R.id.view4)).setText(searchSession.blockingGetSearchResult(3).getTitle());
        //mListView = (ListView) findViewById(R.id.image_list);
        //mListView.setAdapter(new Adapter(this, );


    }

    private class Adapter extends BaseAdapter {
        private Context mContext;
        private GImageSearchSession mSearchSession;

        public Adapter(Context context, GImageSearchSession searchSession) {
            mContext = context;
            mSearchSession = searchSession;
        }

        @Override
        public int getCount() {
            return 100;
        }

        @Override
        public Object getItem(int position) {
            return mSearchSession.blockingGetSearchResult(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(mContext);
            }
            else {
                textView = (TextView) convertView;
            }
            GISearchResult result = (GISearchResult) getItem(position);
            textView.setText(result.getTitle());
            return textView;
        }
    }
}
