package com.backbase.interview.citieslist.main;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import com.backbase.interview.citieslist.PersistSortedCitiesService;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.models.entities.Coordination;
import com.backbase.interview.citieslist.ui.activities.BaseActivity;
import com.backbase.interview.citieslist.utils.CityManager;
import com.backbase.interview.citieslist.utils.Constants;
import com.backbase.interview.citieslist.utils.EndlessRecyclerViewScrollListener;
import com.backbase.interview.citieslist.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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

public class MainActivity extends BaseActivity {

  private RecyclerView mRecyclerView;
  private ProgressBar mLoadingProgressBar;
  private EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initUI();

    new CitiesLoader(this).execute();
  }

  private void initUI() {

    mLoadingProgressBar = findViewById(R.id.main_activity_pb);

    mRecyclerView = findViewById(R.id.main_activity_rv);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mRecyclerView.setLayoutManager(
        linearLayoutManager);
    mRecyclerView.setAdapter(new CitiesAdapter());
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

    mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
      @Override public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
        Timber.d("loadMore(), page:[%s], totalItemsCount:[%s]", page, totalItemsCount);
      }
    };
    mRecyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);

    MenuItem item = menu.findItem(R.id.action_search);
    SearchView searchView = (SearchView) item.getActionView();
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(String query) {
        Timber.w("onQueryTextSubmit:[%s]", query);
        return true;
      }

      @Override public boolean onQueryTextChange(String newText) {
        Timber.w("onQueryTextChange:[%s]", newText);
        ((CitiesAdapter) (mRecyclerView.getAdapter())).filter(newText);

        return true;
      }
    });

    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override public boolean onClose() {
        ((CitiesAdapter) (mRecyclerView.getAdapter())).filter("");

        return false;
      }
    });

    return true;
  }

  static class CitiesLoader extends AsyncTask<Void, Integer, List<City>> {
    final WeakReference<MainActivity> mContextWeakReference;

    CitiesLoader(final MainActivity context) {
      this.mContextWeakReference = new WeakReference<>(context);
    }

    @Override protected void onPreExecute() {
      Timber.w("onPreExecute");
      super.onPreExecute();
      if (mContextWeakReference.get() != null) {
        mContextWeakReference.get().showLoading();
      }
    }

    private long lastNotifiedProgress = 0;

    @Override protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);

      if (System.currentTimeMillis() - lastNotifiedProgress > 5_000) {
        lastNotifiedProgress = System.currentTimeMillis();
        Timber.d("progress:[%s:%s]", values[0], (values[0] / 209557f) * 100);
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
      Timber.w("deserializeParse() #%s items in duration:[%sms]", citiesList.size(),
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processStartTime));

      jsonReader.endArray();
      jsonReader.close();

      return citiesList;
    }

    private List<City> typeTokenParse(JsonReader jsonReader, boolean isSorted) throws IOException {
      final List<City> citiesList = new LinkedList<>();

      jsonReader.beginArray();

      final long processStartTime = System.nanoTime();
      while (jsonReader.hasNext()) {

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
      Timber.w("typeTokenParse() #%s items in duration:[%sms]", citiesList.size(),
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processStartTime));

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

      Timber.w("sortAsc() took:[%sms]",
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sortStartTime));
    }
    private void saveSortedCitiesList(List<City> citiesList) {
      Timber.d("saveSortedCitiesList()");

      JsonWriter writer;


      final long writeStartTime = System.nanoTime();

      try {
        if(mContextWeakReference.get() == null) return;
        File sortedCitiesFile = FileUtils.getSortedCitiesFile(mContextWeakReference.get());

        if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
          return;
        }
        writer = new JsonWriter(new FileWriter(
            sortedCitiesFile));
        writer.beginArray();

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

        Timber.w("saveSortedCitiesList() #%s items took [%sms] in:[%s]", citiesList.size(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - writeStartTime), sortedCitiesFile.getAbsolutePath());
        writer.endArray();
        writer.close();


      } catch (IOException e) {
        System.err.print(e.getMessage());
      }
    }

    @Override protected List<City> doInBackground(Void... voids) {
      Timber.w("doInBackground");

      final List<City> citiesList = new LinkedList<>();

      try {
        if (mContextWeakReference.get() == null) return null;

        final File sortedCitiesFile = FileUtils.getSortedCitiesFile(mContextWeakReference.get());
        if (sortedCitiesFile.exists() && sortedCitiesFile.length() > 0) {
          final InputStream inputStream = new FileInputStream(sortedCitiesFile);
          final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
          citiesList.addAll(typeTokenParse(jsonReader, true));
        } else {
          final InputStream inputStream = mContextWeakReference.get().getAssets().open(FileUtils.RAW_CITIES_FILE_NAME);
          final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

          //citiesList.addAll(deserializeParse(jsonReader));
          citiesList.addAll(typeTokenParse(jsonReader, false));
          sortAsc(citiesList);

          CityManager.getInstance().citiesList.clear();
          CityManager.getInstance().citiesList.addAll(citiesList);
          mContextWeakReference.get()
              .startService(
                  new Intent(mContextWeakReference.get(), PersistSortedCitiesService.class));

          //saveSortedCitiesList(citiesList);

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


    @Override protected void onPostExecute(List<City> cities) {
      Timber.w("onPostExecute");

      super.onPostExecute(cities);

      if (mContextWeakReference.get() != null) {
        mContextWeakReference.get().hideLoading();
        mContextWeakReference.get().bindRecyclerViewData(cities);
      }
    }
  }

  private void showLoading() {
    mLoadingProgressBar.setVisibility(View.VISIBLE);
  }

  private void hideLoading() {
    mLoadingProgressBar.setVisibility(View.GONE);
  }

  private void bindRecyclerViewData(List<City> cities) {
    ((CitiesAdapter) (mRecyclerView.getAdapter())).addAll(cities);
  }
}
