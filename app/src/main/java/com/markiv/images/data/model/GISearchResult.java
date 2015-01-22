package com.markiv.images.data.model;

/**
 * An object representing a single search result
 * @author vikrambd
 * @since 1/20/15
 */
public class GISearchResult {
    private String titleNoFormatting;
    private String url;

    private int height;
    private int width;

    public GISearchResult() {
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setTitle(String title) {
        this.titleNoFormatting = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getTitle() {
        return titleNoFormatting;
    }

    public String getUrl() {
        return url;
    }
}
