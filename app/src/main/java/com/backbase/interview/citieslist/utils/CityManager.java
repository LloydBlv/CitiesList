package com.backbase.interview.citieslist.utils;

import com.backbase.interview.citieslist.models.entities.City;
import java.util.LinkedList;
import java.util.List;

public class CityManager {
  private static CityManager ourInstance;

  public final List<City> citiesList = new LinkedList<>();


  public static CityManager getInstance() {
    if (ourInstance == null) {
      ourInstance = new CityManager();
    }
    return ourInstance;
  }

  private CityManager() {
  }
}
