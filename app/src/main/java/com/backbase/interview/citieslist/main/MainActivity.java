package com.backbase.interview.citieslist.main;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.ui.activities.BaseActivity;
import com.backbase.interview.citieslist.utils.EndlessRecyclerViewScrollListener;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class MainActivity extends BaseActivity {

  private RecyclerView mRecyclerView;
  private CitiesAdapter mCitiesAdapter;
  private ProgressBar mLoadingProgressBar;
  private EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener;

  private boolean isFirstTimeSearchOpen = true;
  private CityListTask mCityListTask;
  private CityLoadMoreTask mCityLoadMoreTask;
  private CitySearchTask mCitySearchTask;

  private ProgressBar mSearchProgressBar;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initUI();

    mCityListTask = new CityListTask(this);
    mCityListTask.execute();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (mCityListTask != null) {
      mCityListTask.cancel(true);
      mCityListTask = null;
    }

    if (mCityLoadMoreTask != null) {
      mCityLoadMoreTask.cancel(true);
      mCityLoadMoreTask = null;
    }
    if (mCitySearchTask != null) {
      mCitySearchTask.cancel(true);
      mCitySearchTask = null;
    }
  }

  private void initUI() {

    mLoadingProgressBar = findViewById(R.id.main_activity_pb);

    mRecyclerView = findViewById(R.id.main_activity_rv);
    LinearLayoutManager linearLayoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mRecyclerView.setLayoutManager(linearLayoutManager);
    mCitiesAdapter = new CitiesAdapter();
    mRecyclerView.setAdapter(mCitiesAdapter);
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

    mEndlessRecyclerViewScrollListener =
        new EndlessRecyclerViewScrollListener(linearLayoutManager) {
          @Override public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
            Timber.d("loadMore(), page:[%s], totalItemsCount:[%s], isSearchViewOpen:[%s]", page, totalItemsCount, isSearchViewOpen);
            if(isSearchViewOpen) return;
            mCityLoadMoreTask = new CityLoadMoreTask(MainActivity.this);
            mCityLoadMoreTask.execute(page);
          }
        };
    mRecyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
  }

  boolean isSearchViewOpen = false;
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);

    MenuItem mSearchMenuItem = menu.findItem(R.id.action_search);
    final SearchView searchView = (SearchView) mSearchMenuItem.getActionView();

    mSearchProgressBar = searchView.findViewById(R.id.search_progressbar);
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(String query) {
        Timber.w("onQueryTextSubmit:[%s]", query);
        return true;
      }

      @Override public boolean onQueryTextChange(String newText) {

        if(!isSearchViewOpen) return false;
        else if(isFirstTimeSearchOpen){
          isFirstTimeSearchOpen = false;
          return false;
        } else if (newText.isEmpty()) {
          mCitiesAdapter.clearSearchList();
          return true;
        }

        Timber.w("onQueryTextChange:[%s]", newText);
        //((CitiesAdapter) (mRecyclerView.getAdapter())).filter(newText);
        mCitiesAdapter.clearSearchList();

        if (mCitySearchTask != null) {
          mCitySearchTask.cancel(true);
          mCitySearchTask = null;
        }
        mCitySearchTask = new CitySearchTask(MainActivity.this);
        mCitySearchTask.execute(newText);

        return true;
      }
    });

    mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override public boolean onMenuItemActionExpand(MenuItem item) {
        Timber.d("onMenuItemActionExpand");
        isSearchViewOpen = true;
        mCitiesAdapter.clearSearchList();
        return true;
      }

      @Override public boolean onMenuItemActionCollapse(MenuItem item) {
        Timber.d("onMenuItemActionCollapse");

        isSearchViewOpen = false;

        mCitiesAdapter.showCitiesList();
        //((CitiesAdapter) (mRecyclerView.getAdapter())).filter("");
        return true;
      }
    });
    return true;
  }

  public void showLoading() {
    mLoadingProgressBar.setVisibility(View.VISIBLE);
  }

  public void hideLoading() {
    mLoadingProgressBar.setVisibility(View.GONE);
  }

  public void bindRecyclerViewData(List<City> cities) {
    mCitiesAdapter.addAll(cities);
  }

  public void setSearchResultData(List<City> cities) {
    mCitiesAdapter.setSearchResult(cities);
    if (cities.isEmpty()) {
      Toast.makeText(this, "No city found", Toast.LENGTH_SHORT).show();
    }

  }

  public void showSearchProgress() {
    mSearchProgressBar.setVisibility(View.VISIBLE);
  }

  public void hideSearchProgress() {
    mSearchProgressBar.setVisibility(View.GONE);
  }
}
