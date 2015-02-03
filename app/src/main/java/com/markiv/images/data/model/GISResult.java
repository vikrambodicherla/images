package com.markiv.images.data.model;

/**
 * An object representing a single search result
 * @author vikrambd
 * @since 1/20/15
 */
public class GISResult {
    private String titleNoFormatting;

    private String tbUrl;
    private int tbHeight;
    private int tbWidth;

    public GISResult() {
    }

    public String getTitleNoFormatting() {
        return titleNoFormatting;
    }

    public void setTitleNoFormatting(String titleNoFormatting) {
        this.titleNoFormatting = titleNoFormatting;
    }

    public String getTbUrl() {
        return tbUrl;
    }

    public void setTbUrl(String tbUrl) {
        this.tbUrl = tbUrl;
    }

    public int getTbHeight() {
        return tbHeight;
    }

    public void setTbHeight(int tbHeight) {
        this.tbHeight = tbHeight;
    }

    public int getTbWidth() {
        return tbWidth;
    }

    public void setTbWidth(int tbWidth) {
        this.tbWidth = tbWidth;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GISResult) {
            GISResult other = (GISResult) o;
            return titleNoFormatting != null && other.titleNoFormatting != null
                    && titleNoFormatting.equals(other.titleNoFormatting) &&
                    tbUrl != null && other.tbUrl != null && tbUrl.equals(other.tbUrl) &&
                    tbHeight == other.tbHeight && tbWidth == other.tbWidth;
        }
        else {
            return false;
        }
    }
}
