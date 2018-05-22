package com.jamalsafwat.wear2test.pojo;

/**
 * Represents planet for app.
 */
public class Planet {

  private String name;
  private String navigationIcon;
  private String image;
  private String moons;
  private String volume;
  private String surfaceArea;

  public Planet(
          String name,
          String navigationIcon,
          String image,
          String moons,
          String volume,
          String surfaceArea) {

    this.name = name;
    this.navigationIcon = navigationIcon;
    this.image = image;
    this.moons = moons;
    this.volume = volume;
    this.surfaceArea = surfaceArea;
  }

  public String getName() {
    return name;
  }

  public String getNavigationIcon() {
    return navigationIcon;
  }

  public String getImage() {
    return image;
  }

  public String getMoons() {
    return moons;
  }

  public String getVolume() {
    return volume;
  }

  public String getSurfaceArea() {
    return surfaceArea;
  }
}