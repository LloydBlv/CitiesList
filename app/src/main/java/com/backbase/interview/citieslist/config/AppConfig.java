package com.backbase.interview.citieslist.config;

import android.app.Application;
import timber.log.Timber;

public class AppConfig extends Application{
  @Override public void onCreate() {
    super.onCreate();
    Timber.plant(new Timber.DebugTree());
  }
}
