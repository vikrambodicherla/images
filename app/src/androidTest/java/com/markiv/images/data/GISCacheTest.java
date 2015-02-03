package com.markiv.images.data;

import com.markiv.images.data.model.GISResult;

import junit.framework.Assert;

import org.fest.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * @author vikrambd
 * @since 2/2/15
 */
@RunWith(RobolectricTestRunner.class)
public class GISCacheTest {

    @Test
    public void testGetEntry_whenPut(){
        GISResult result = new GISResult();
        result.setTitleNoFormatting("Sample title");

        GISCache cache = new GISCache();
        cache.put(0, result);

        Assert.assertEquals(result, cache.get(0));
    }

    @Test
    public void testGetEntry_whenBatchPut(){
        GISResult result1 = new GISResult();
        result1.setTitleNoFormatting("Sample title");

        GISResult result2 = new GISResult();
        result2.setTitleNoFormatting("Sample title2");

        GISCache cache = new GISCache();
        cache.batchPut(3, Lists.newArrayList(result1, result2).iterator());

        Assert.assertEquals(result1, cache.get(3));
        Assert.assertEquals(result2, cache.get(4));
    }
}
