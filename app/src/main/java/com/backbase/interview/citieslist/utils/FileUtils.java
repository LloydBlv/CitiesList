package com.backbase.interview.citieslist.utils;

import android.content.Context;
import java.io.File;

public class FileUtils {
  public static final String SORTED_CITIES_FILE_NAME = "sorted_cities.json";
  public static final String RAW_CITIES_FILE_NAME = "cities.json";

  public static File getSortedCitiesFile(Context context) {
    return
        new File(context.getExternalCacheDir(), FileUtils.SORTED_CITIES_FILE_NAME);
  }
}
