package com.backbase.interview.citieslist.main;

import com.backbase.interview.citieslist.models.entities.City;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MainViewContract {
  void showLoading();

  void hideLoading();

  void showSearchProgress();

  void hideSearchProgress();

  void bindRecyclerViewData(List<City> cityList);

  void setSearchResultData(List<City> cityList);

  void startPersistService();

  InputStream getCityInputStream() throws IOException;

  File getSortedCitiesFile();
}
