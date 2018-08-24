package com.backbase.interview.citieslist;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.utils.CityManager;
import com.backbase.interview.citieslist.utils.FileUtils;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class PersistSortedCitiesService extends IntentService {
  public PersistSortedCitiesService() {
    super("PersistSortedCitiesService");
  }

  @Override protected void onHandleIntent(@Nullable Intent intent) {
    JsonWriter writer;


    try {
      File sortedCitiesFile = FileUtils.getSortedCitiesFile(this);

      if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
        return;
      }
      writer = new JsonWriter(new FileWriter(
          sortedCitiesFile));
      writer.beginArray();

      final List<City> citiesList = CityManager.getInstance().citiesList;
      for (City city : citiesList) {
        writer.beginObject();
        writer.name("country").value(city.country);
        writer.name("name").value(city.name);
        writer.name("coord");
        writer.beginObject();
        writer.name("lon").value(city.coord.lon);
        writer.name("lat").value(city.coord.lat);
        writer.endObject();
        writer.endObject();
      }

      writer.endArray();
      writer.close();


    } catch (IOException e) {
      System.err.print(e.getMessage());
    }

  }
}
