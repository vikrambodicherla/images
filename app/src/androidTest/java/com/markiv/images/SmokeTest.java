package com.markiv.images;

import android.app.SearchManager;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.markiv.images.ui.SearchActivity;
import com.robotium.solo.Solo;

public class SmokeTest extends ActivityInstrumentationTestCase2<SearchActivity> {
    private static final String SMOKE_QUERY = "uber";

    private Solo solo;

    public SmokeTest() {
        super(SearchActivity.class);

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, SMOKE_QUERY);
        setActivityIntent(intent);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testDummy(){
        //This test only exists because we need at least one test
        assertEquals(true, true);
    }
}