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
}
