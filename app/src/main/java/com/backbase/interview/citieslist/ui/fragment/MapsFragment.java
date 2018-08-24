package com.backbase.interview.citieslist.ui.fragment;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsFragment extends SupportMapFragment implements OnMapReadyCallback {

  public static final String ARG_LATITUDE = "arg_latitude";
  public static final String ARG_LONGITUDE = "arg_longitude";
  public static final String ARG_NAME = "arg_name";

  public static MapsFragment newInstance(final String name, final double lat, final double lon) {
    MapsFragment mapsFragment = new MapsFragment();
    final Bundle args = new Bundle();
    args.putDouble(ARG_LATITUDE, lat);
    args.putDouble(ARG_LONGITUDE, lon);
    args.putString(ARG_NAME, name);
    mapsFragment.setArguments(args);
    return mapsFragment;
  }

  @Override public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setHasOptionsMenu(false);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    menu.clear();
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);

    getMapAsync(this);
  }

  @Override public void onMapReady(GoogleMap googleMap) {
    LatLng currentCity =
        new LatLng(getArguments().getDouble(ARG_LATITUDE), getArguments().getDouble(ARG_LONGITUDE));

    googleMap.addMarker(new MarkerOptions().position(currentCity)
        .title(getArguments().getString(ARG_NAME)));
    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
        currentCity, 17f));
  }
}
