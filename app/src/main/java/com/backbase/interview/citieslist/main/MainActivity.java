package com.backbase.interview.citieslist.main;

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
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.models.entities.Coordination;
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
        Timber.d("progress:[%s:%s]", values[0], (values[0]/209557f)*100);

      }
    }

    @Override protected List<City> doInBackground(Void... voids) {
      Timber.w("doInBackground");

      final List<City> citiesList = new LinkedList<>();

      try {
        if (mContextWeakReference.get() == null) return null;
        final InputStream inputStream = mContextWeakReference.get().getAssets().open("cities.json");
        final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));


        final Gson gson = new GsonBuilder().create();

        int currentIndex = 0;


        jsonReader.beginArray();

        final long processStartTime = System.nanoTime();
        while (jsonReader.hasNext()) {

          final City currentCity = gson.fromJson(jsonReader, City.class);
          citiesList.add(currentCity);
          //publishProgress(currentIndex++);

          //publishProgress(currentIndex++);
          //currentIndex++;
          //if (currentIndex >= 1000) {
          //  break;
          //}
        }
        Timber.w("processed:[%s records] in duration:[%s]",citiesList.size(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processStartTime));

        jsonReader.endArray();
        jsonReader.close();

        final long sortStartTime = System.nanoTime();
        Collections.sort(citiesList, new Comparator<City>() {
          @Override public int compare(City o1, City o2) {
            int cityNameCompare = o1.name.compareTo(o2.name);
            if(cityNameCompare != 0)
            return cityNameCompare;

            return o1.country.compareTo(o2.country);
          }
        });

        Timber.w("sort_duration:[%s]",
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sortStartTime));
      } catch (Exception ex) {
        Timber.e(ex, "while parse");
        //ex.printStackTrace();
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
