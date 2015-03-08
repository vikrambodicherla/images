
package com.markiv.gis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.markiv.gis.api.model.APIResponse;
import com.markiv.gis.api.model.APIResult;

/**
 * @author vikrambd
 * @since 3/7/15
 */
public class Page {
    private final int mStart;
    private final int mCount;
    private final List<Item> mItems = new ArrayList<Item>();
    private final int mEstimatedResultCount;

    Page(int start, int count, APIResponse apiResponse) {
        mStart = start;
        mCount = count;

        for (APIResult result : apiResponse.responseData.results) {
            mItems.add(new Item(result));
        }

        mEstimatedResultCount = apiResponse.responseData.cursor.estimatedResultCount;
    }

    public boolean isEmpty(){
        return mItems.isEmpty();
    }

    public int getEstimatedResultCount() {
        return mEstimatedResultCount;
    }

    public int getStart() {
        return mStart;
    }

    public int getCount() {
        return mCount;
    }

    public Item getItem(int position) {
        return mItems.get(position);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(mItems);
    }

    @Override
    public String toString() {
        return "[" + mStart + ", " + mCount + "]: " + mItems.size();
    }

    public class Item {
        private final APIResult mAPIResult;

        public Item(APIResult APIResult) {
            mAPIResult = APIResult;
        }

        public String getTitle() {
            return mAPIResult.getTitleNoFormatting();
        }

        public String getUrl() {
            return mAPIResult.getTbUrl();
        }

        @Override
        public String toString() {
            return getTitle();
        }
    }
}
