package com.backbase.interview.citieslist.main;

import android.os.Bundle;
import com.backbase.interview.citieslist.R;
import com.backbase.interview.citieslist.ui.activities.BaseActivity;

public class MainActivity extends BaseActivity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_activity_frame_container, MainFragment.newInstance())
        .commit();


  }
}
