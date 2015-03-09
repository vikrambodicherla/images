package com.markiv.images.ui.history;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.Robolectric;

/**
 * @author vikrambd
 * @since 3/8/15
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchHistoryManagerTest {
    SearchHistoryManager mSubject;

    @Test
    public void testGetList_whenEmpty(){
        mSubject = new SearchHistoryManager(Robolectric.getShadowApplication().getApplicationContext());
        Assert.assertEquals(0, mSubject.getSearchHistory().size());
    }
}