package com.markiv.gis.api.model;

import java.util.Iterator;
import java.util.List;

/**
 * Object representing the search API call response
 * @author vikrambd
 * @since 1/20/15
 */
public class APIResponse {
    //These fields are not a part of the response
    public transient String query;
    public transient int start;
    public transient int rsz;

    public ResponseData responseData;
    public String responseDetails;
    public int responseStatus;

    public class ResponseData {
        public List<APIResult> results;
        public Cursor cursor;
    }

    public class Cursor {
        public int estimatedResultCount;
    }

    public boolean isSuccess(){
        return responseStatus == 200 && responseDetails == null;
    }

    public String getErrorMessage(){
        return responseDetails;
    }

    public Iterator<APIResult> getSearchResults() {
        if(responseData.results != null){
            return responseData.results.iterator();
        }
        else {
            return null;
        }
    }
}
