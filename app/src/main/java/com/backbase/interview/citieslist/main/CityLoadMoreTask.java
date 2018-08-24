package com.backbase.interview.citieslist.main;

import android.os.AsyncTask;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.models.entities.Coordination;
import com.backbase.interview.citieslist.utils.CityManager;
import com.backbase.interview.citieslist.utils.Constants;
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

public  class CityLoadMoreTask extends AsyncTask<Integer, Void, List<City>> {
  private final WeakReference<MainViewContract> mViewReference;

  CityLoadMoreTask(MainViewContract mainViewContract) {
    mViewReference = new WeakReference<>(mainViewContract);
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

    Timber.w("sortAsc() took:[%sms]",
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sortStartTime));
  }

  @Override protected List<City> doInBackground(Integer... integers) {
    final int pageIndex = integers[0];
    Timber.w("doInBackground(), pageIndex:[%s]", pageIndex);

    final List<City> citiesList = new LinkedList<>();

    try {
      if (mViewReference.get() == null) return null;

      final File sortedCitiesFile = mViewReference.get().getSortedCitiesFile();
      //final File sortedCitiesFile = FileUtils.getSortedCitiesFile(mViewReference.get());

      if (!CityManager.getInstance().citiesList.isEmpty()) {
        Timber.d("CityManager size:[%s], subIndex:[%s -> %s]", CityManager.getInstance().citiesList.size(),
            pageIndex * Constants.PAGE_SIZE,
            Constants.PAGE_SIZE);
        List<City> subList =
            CityManager.getInstance().citiesList.subList(pageIndex * Constants.PAGE_SIZE,
                pageIndex * Constants.PAGE_SIZE + Constants.PAGE_SIZE);

        Timber.d("City manager load more:[%s]", subList.size());
        citiesList.addAll(
            subList);
      } else if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
        Timber.d("reading from sorted json file");
        final InputStream inputStream = new FileInputStream(sortedCitiesFile);
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        citiesList.addAll(typeTokenParse(jsonReader, pageIndex));
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

  private List<City> typeTokenParse(JsonReader jsonReader, int pageIndex) throws IOException {
    final List<City> citiesList = new LinkedList<>();

    jsonReader.beginArray();

    final long processStartTime = System.nanoTime();

    int currentIndex = 0;
    while (jsonReader.hasNext()) {

      if (isCancelled()) {
        break;
      }

      if (currentIndex < pageIndex * Constants.PAGE_SIZE) {
        jsonReader.skipValue();
        currentIndex++;
        continue;
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

      if (citiesList.size() >= Constants.PAGE_SIZE) {
        break;
      }
    }
    Timber.w("loadMore.typeTokenParse() #%s items in duration:[%sms]", citiesList.size(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processStartTime));

    //jsonReader.endObject();
    jsonReader.close();

    return citiesList;
  }

  @Override protected void onPostExecute(List<City> cities) {
    super.onPostExecute(cities);
    Timber.d("onPostExecute(), size:[%s]", cities.size());
    if (mViewReference.get() != null) {
      mViewReference.get().bindRecyclerViewData(cities);
    }
  }
}