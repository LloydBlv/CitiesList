package com.backbase.interview.citieslist.ui.fragment;

import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapsFragment extends SupportMapFragment implements OnMapReadyCallback {

  public static final String ARG_LATITUDE = "arg_latitude";
  public static final String ARG_LONGITUDE = "arg_longitude";

  public static MapsFragment newInstance(final double lat, final double lon) {
    MapsFragment mapsFragment = new MapsFragment();
    final Bundle args = new Bundle();
    args.putDouble(ARG_LATITUDE, lat);
    args.putDouble(ARG_LONGITUDE, lon);
    mapsFragment.setArguments(args);
    return mapsFragment;
  }

  @Override public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);

    getMapAsync(this);
  }

  @Override public void onMapReady(GoogleMap googleMap) {

    googleMap.moveCamera(CameraUpdateFactory.newLatLng(
        new LatLng(getArguments().getDouble(ARG_LATITUDE),
            getArguments().getDouble(ARG_LONGITUDE))));
  }
}
