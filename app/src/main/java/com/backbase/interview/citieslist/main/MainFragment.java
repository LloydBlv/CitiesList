package com.backbase.interview.citieslist.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.backbase.interview.citieslist.PersistSortedCitiesService;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.models.entities.City;
import com.backbase.interview.citieslist.ui.fragment.MapsFragment;
import com.backbase.interview.citieslist.utils.EndlessRecyclerViewScrollListener;
import com.backbase.interview.citieslist.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import timber.log.Timber;

public class MainFragment extends Fragment implements MainViewContract {

  public static MainFragment newInstance(){
    return new MainFragment();
  }

  private RecyclerView mRecyclerView;
  private CitiesAdapter mCitiesAdapter;
  private ProgressBar mLoadingProgressBar;
  private EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener;

  private boolean isFirstTimeSearchOpen = true;
  private CityListTask mCityListTask;
  private CityLoadMoreTask mCityLoadMoreTask;
  private CitySearchTask mCitySearchTask;

  private ProgressBar mSearchProgressBar;


  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main_layout, container, false);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initUI(view);


    mCityListTask = new CityListTask(this);
    mCityListTask.execute();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
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



  private void initUI(final View view) {

    mLoadingProgressBar = view.findViewById(R.id.main_fragment_pb);

    mRecyclerView = view.findViewById(R.id.main_fragment_rv);
    LinearLayoutManager linearLayoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    mRecyclerView.setLayoutManager(linearLayoutManager);
    mCitiesAdapter = new CitiesAdapter(new CitiesAdapter.OnListItemClickListener() {
      @Override public void onListItemClicked(View itemView) {
        int clickedPos = mRecyclerView.getChildAdapterPosition(itemView);
        City item = mCitiesAdapter.getItem(clickedPos);

        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.main_activity_frame_container,
                MapsFragment.newInstance(item.name, item.coord.lat, item.coord.lon))
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit();
      }
    });
    mRecyclerView.setAdapter(mCitiesAdapter);
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));

    mEndlessRecyclerViewScrollListener =
        new EndlessRecyclerViewScrollListener(linearLayoutManager) {
          @Override public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
            Timber.d("loadMore(), page:[%s], totalItemsCount:[%s], isSearchViewOpen:[%s]", page, totalItemsCount, isSearchViewOpen);
            if(isSearchViewOpen) return;
            mCityLoadMoreTask = new CityLoadMoreTask(MainFragment.this);
            mCityLoadMoreTask.execute(page);
          }
        };
    mRecyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
  }

  boolean isSearchViewOpen = false;
  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    menu.clear();
    inflater.inflate(R.menu.menu_main, menu);

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
        mCitySearchTask = new CitySearchTask(MainFragment.this);
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
      Toast.makeText(getActivity(), "No city found", Toast.LENGTH_SHORT).show();
    }

  }

  public void showSearchProgress() {
    mSearchProgressBar.setVisibility(View.VISIBLE);
  }

  public void hideSearchProgress() {
    mSearchProgressBar.setVisibility(View.GONE);
  }

  @Override public void startPersistService() {
    getActivity().startService(new Intent(getActivity(), PersistSortedCitiesService.class));
  }

  @Override public InputStream getCityInputStream() throws IOException {
    return getActivity().getAssets().open(FileUtils.RAW_CITIES_FILE_NAME);
  }

  @Override public File getSortedCitiesFile() {
    return FileUtils.getSortedCitiesFile(getActivity());
  }
}
