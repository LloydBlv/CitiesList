package com.backbase.interview.citieslist.models.entities;

public class City {
  public String name;
  public String country;
  public Coordination coord;

  public City(String name, String country, Coordination coord) {
    this.name = name;
    this.country = country;
    this.coord = coord;
  }

  public static City from(String name, String country, Coordination coord) {
    return new City(name, country, coord);
  }
}
