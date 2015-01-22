package com.markiv.images.data.model;

import java.util.Iterator;
import java.util.List;

/**
 * Object representing the search API call response
 * @author vikrambd
 * @since 1/20/15
 */
public class GISearchResponse {
    //These fields are not a part of the response
    public transient int start;
    public transient int rsz;

    public ResponseData responseData;
    public String responseDetails;
    public int responseStatus;

    public class ResponseData {
        public List<GISearchResult> results;
        public Cursor cursor;
    }

    public class Cursor {
        public int currentPageIndex;
        public int estimatedResultCount;
        public String moreResultsUrl;
        public List<Page> pages;
    }

    public class Page {
        public String label;
        public String start;
    }

    public boolean isSuccess(){
        return responseStatus == 200 && responseDetails == null;
    }

    public String getErrorMessage(){
        return responseDetails;
    }

    public Iterator<GISearchResult> getSearchResults() {
        if(responseData.results != null){
            return responseData.results.iterator();
        }
        else {
            return null;
        }
    }
}
