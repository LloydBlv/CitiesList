package com.backbase.interview.citieslist.models.entities;


public class Coordination {
  public double lat;
  public double lon;

  public Coordination(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
  }

  public static Coordination from(double lat, double lon) {
    return new Coordination(lat, lon);
  }
}
