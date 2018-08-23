package com.backbase.interview.citieslist.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.ui.activities.BaseActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
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

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initUI();

    new CitiesLoader(this).execute();
  }

  private void initUI() {

    mLoadingProgressBar = findViewById(R.id.main_activity_pb);

    mRecyclerView = findViewById(R.id.main_activity_rv);
    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setAdapter(new CitiesAdapter());
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
  }


  static class CitiesLoader extends AsyncTask<Void, Void, List<City>>{
    final WeakReference<MainActivity> mContextWeakReference;

    CitiesLoader(final MainActivity context) {
      this.mContextWeakReference = new WeakReference<>(context);
    }

    @Override protected void onPreExecute() {
      super.onPreExecute();
      if (mContextWeakReference.get() != null) {
        mContextWeakReference.get().showLoading();
      }
    }

    @Override protected List<City> doInBackground(Void... voids) {
      final List<City> citiesList = new LinkedList<>();

      try {
        if(mContextWeakReference.get() == null) return null;
        final InputStream inputStream = mContextWeakReference.get().getAssets().open("cities.json");
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

        jsonReader.beginArray();

        final Gson gson = new GsonBuilder().create();

        Timber.d("parse_started_at:[%s]", System.nanoTime());
        final long processStartTime = System.nanoTime();
        while (jsonReader.hasNext()) {
          final City currentCity = gson.fromJson(jsonReader, City.class);
          citiesList.add(currentCity);
        }
        Timber.d("process_duration:[%s]",
            TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - processStartTime));
        Timber.d("parsed %s records", citiesList.size());

        jsonReader.endArray();
        jsonReader.close();

        final long sortStartTime = System.nanoTime();
        Collections.sort(citiesList, new Comparator<City>() {
          @Override public int compare(City o1, City o2) {
            return o1.name.compareTo(o2.name);
          }
        });

        Timber.d("sort_duration:[%s]",
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sortStartTime));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      return citiesList;
    }

    @Override protected void onPostExecute(List<City> cities) {
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
