package com.backbase.interview.citieslist.main;

import android.os.AsyncTask;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.models.entities.Coordination;
import com.backbase.interview.citieslist.utils.CityManager;
import com.backbase.interview.citieslist.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public  class CityListTask extends AsyncTask<Void, Integer, List<City>> {
  private final WeakReference<MainViewContract> mViewReference;

  CityListTask(final MainViewContract mainViewContract) {
    this.mViewReference = new WeakReference<>(mainViewContract);
  }

  @Override protected void onPreExecute() {
    super.onPreExecute();
    if (mViewReference.get() != null) {
      mViewReference.get().showLoading();
    }
  }

  private long lastNotifiedProgress = 0;

  @Override protected void onProgressUpdate(Integer... values) {
    super.onProgressUpdate(values);

    if (System.currentTimeMillis() - lastNotifiedProgress > 5_000) {
      lastNotifiedProgress = System.currentTimeMillis();
    }
  }

  private List<City> deserializeParse(JsonReader jsonReader) throws IOException {
    final List<City> citiesList = new LinkedList<>();
    final Gson gson = new GsonBuilder().create();
    jsonReader.beginArray();
    final long processStartTime = System.nanoTime();
    while (jsonReader.hasNext()) {
      final City currentCity = gson.fromJson(jsonReader, City.class);
      citiesList.add(currentCity);
    }

    jsonReader.endArray();
    jsonReader.close();

    return citiesList;
  }

  private List<City> typeTokenParse(JsonReader jsonReader, boolean isSorted) throws IOException {
    final List<City> citiesList = new LinkedList<>();

    jsonReader.beginArray();

    final long processStartTime = System.nanoTime();
    while (jsonReader.hasNext()) {

      if (isCancelled()) {
        break;
      }

      jsonReader.beginObject();
      jsonReader.nextName();
      final String country = jsonReader.nextString();

      jsonReader.nextName();
      final String name = jsonReader.nextString();


      String maybeId = jsonReader.nextName();

      //Timber.d("name:[%s], toSt:[%s], maybeId:[%s]", jsonReader.peek().name(), jsonReader.peek().toString(), maybeId);

      if (maybeId.equals("_id")) {
        jsonReader.nextInt();

        jsonReader.nextName();
        jsonReader.beginObject();
      } else {
        jsonReader.beginObject();
      }



      //jsonReader.nextName();
      //jsonReader.beginObject();

      jsonReader.nextName();
      final double longitude = jsonReader.nextDouble();
      jsonReader.nextName();
      final double latitude = jsonReader.nextDouble();
      jsonReader.endObject();
      jsonReader.endObject();

      citiesList.add(City.from(name, country, Coordination.from(latitude, longitude)));

      if (isSorted && citiesList.size() >= Constants.PAGE_SIZE) {
        break;
      }
    }
    if (!isSorted) {
      jsonReader.endArray();
    }
    //jsonReader.endObject();
    jsonReader.close();

    return citiesList;
  }

  private void sortAsc(final List<City> citiesList) {
    final long sortStartTime = System.nanoTime();
    Collections.sort(citiesList, new Comparator<City>() {
      @Override public int compare(City o1, City o2) {
        int cityNameCompare = o1.name.compareTo(o2.name);
        if (cityNameCompare != 0) return cityNameCompare;

        return o1.country.compareTo(o2.country);
      }
    });

  }

  @Override protected List<City> doInBackground(Void... voids) {
    final List<City> citiesList = new LinkedList<>();

    try {
      if (mViewReference.get() == null) return null;

      final File sortedCitiesFile = mViewReference.get().getSortedCitiesFile();
      //final File sortedCitiesFile = FileUtils.getSortedCitiesFile(mViewReference.get());
      if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
        final InputStream inputStream = new FileInputStream(sortedCitiesFile);
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        citiesList.addAll(typeTokenParse(jsonReader, true));
      } else {
        final InputStream inputStream = mViewReference.get().getCityInputStream();
        //final InputStream inputStream = mViewReference.get().getAssets().open(FileUtils.RAW_CITIES_FILE_NAME);
        //final InputStream inputStream = mViewReference.get().getAssets().open(FileUtils.RAW_CITIES_FILE_NAME);
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

        //citiesList.addAll(deserializeParse(jsonReader));
        citiesList.addAll(typeTokenParse(jsonReader, false));
        sortAsc(citiesList);

        CityManager.getInstance().citiesList.clear();
        CityManager.getInstance().citiesList.addAll(citiesList);
        mViewReference.get().startPersistService();
        //mViewReference.get()
        //    .startService(
        //        new Intent(mViewReference.get(), PersistSortedCitiesService.class));

        //saveSortedCitiesList(citiesList);

      }


    } catch (Exception ex) {
      //ex.printStackTrace();
    }

    if (citiesList.size() >= Constants.PAGE_SIZE) {
      return citiesList.subList(0, Constants.PAGE_SIZE);
    }
    return citiesList;
  }


  @Override protected void onPostExecute(List<City> cities) {

    super.onPostExecute(cities);

    if (mViewReference.get() != null) {
      mViewReference.get().hideLoading();
      mViewReference.get().bindRecyclerViewData(cities);
    }
  }
}