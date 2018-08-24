package com.backbase.interview.citieslist.main;

import android.os.AsyncTask;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.models.entities.Coordination;
import com.backbase.interview.citieslist.utils.CityManager;
import com.backbase.interview.citieslist.utils.Constants;
import com.backbase.interview.citieslist.utils.FileUtils;
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
import timber.log.Timber;

public  class CitySearchTask extends AsyncTask<String, Void, List<City>> {
  private final WeakReference<MainActivity> mContextWeakReference;

  CitySearchTask(MainActivity mainActivity) {
    mContextWeakReference = new WeakReference<>(mainActivity);
  }

  @Override protected List<City> doInBackground(String... strings) {
    final String  query = strings[0];
    Timber.w("doInBackground(), query:[%s]", query);

    final List<City> citiesList = new LinkedList<>();

    if (query.isEmpty()) {
      return citiesList;
    }

    try {
      if (mContextWeakReference.get() == null) return null;

      final File sortedCitiesFile = FileUtils.getSortedCitiesFile(mContextWeakReference.get());

      if (!CityManager.getInstance().citiesList.isEmpty()) {
        Timber.d("CityManager size:[%s]]", CityManager.getInstance().citiesList.size());

        for (City city : CityManager.getInstance().citiesList) {
          if (city.name.startsWith(query)) {
            citiesList.add(city);
          }
        }

        Timber.d("City manager query:[%s]", citiesList.size());
      } else if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
        Timber.d("reading from sorted json file");
        final InputStream inputStream = new FileInputStream(sortedCitiesFile);
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        citiesList.addAll(typeTokenParse(jsonReader, query));
      } else {
        Timber.d("none");
      }


    } catch (Exception ex) {
      Timber.e(ex, "while parse");
      //ex.printStackTrace();
    }

    if (citiesList.size() >= Constants.PAGE_SIZE) {
      return citiesList.subList(0, Constants.PAGE_SIZE);
    }
    return citiesList;
  }

  private List<City> typeTokenParse(final JsonReader jsonReader, final String query) throws IOException {
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

      //Timber.d("trying:[%s]", name);

      String maybeId = jsonReader.nextName();

      if (maybeId.equals("_id")) {
        jsonReader.nextInt();

        jsonReader.nextName();
        jsonReader.beginObject();
      } else {
        jsonReader.beginObject();
      }

      jsonReader.nextName();
      final double longitude = jsonReader.nextDouble();
      jsonReader.nextName();
      final double latitude = jsonReader.nextDouble();
      jsonReader.endObject();
      jsonReader.endObject();


      //Timber.d("[%s]vs[%s]", name, query);
      if (name.toLowerCase().startsWith(query.toLowerCase())) {
        //Timber.d("was a match:[%s]", name);
        citiesList.add(City.from(name, country, Coordination.from(latitude, longitude)));

      }else if(shouldSkipSearch(query, name)){
      //}else if(((int) name.charAt(0)) > ((int) query.charAt(0))){
        break;
      } else {
        //Timber.d("[%s] did not match", name);
        //
        //jsonReader.skipValue();
        //continue;
      }



      if (citiesList.size() >= Constants.PAGE_SIZE) {
        break;
      }
    }
    Timber.w("CitySearchTask.typeTokenParse() #%s items in duration:[%sms]", citiesList.size(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processStartTime));

    //jsonReader.endObject();
    jsonReader.close();

    return citiesList;
  }

  private boolean shouldSkipSearch(final String query, final String currentCityName) {
    //Timber.w("shouldSkipSearch(%s,%s)", currentCityName, query);
    if(query.length() > currentCityName.length()) return true;
    else if(query.charAt(0) < currentCityName.charAt(0)) return true;
    else if(query.charAt(0) > currentCityName.charAt(0)) return false;
    //else if (query.charAt(0) == currentCityName.charAt(0)) {
    //  return shouldSkipSearch(query.substring(1), currentCityName.substring(1));
    //}
    boolean shouldSkip = false;
    /*for(int i = 0; i < query.length(); i++) {
      Timber.d("i:[%s], [%s]vs[%s] -> [%s], [%s]", i, currentCityName.charAt(i), query.charAt(i),
          ((int) currentCityName.charAt(i)), ((int) query.charAt(i)));
      shouldSkip |= currentCityName.charAt(i) > query.charAt(i);
    }*/
    return shouldSkip;
  }

  @Override protected void onPreExecute() {
    super.onPreExecute();

    if (mContextWeakReference.get() != null) {
      mContextWeakReference.get().showSearchProgress();
    }
  }

  @Override protected void onPostExecute(List<City> cities) {
    super.onPostExecute(cities);
    Timber.d("onPostExecute(), size:[%s]", cities.size());
    if (mContextWeakReference.get() != null) {
      mContextWeakReference.get().setSearchResultData(cities);
      mContextWeakReference.get().hideSearchProgress();
    }
  }
}