package com.jamalsafwat.wear2test.pojo;

/**
 * Created by jamal.safwat on 7/27/2017.
 */

public class IconData {

    private String description;
    private int imgId;

    public IconData(String description, int imgId) {
        this.description = description;
        this.imgId = imgId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getImgId() {
        return imgId;
    }

    public void setImgId(int imgId) {
        this.imgId = imgId;
    }
}