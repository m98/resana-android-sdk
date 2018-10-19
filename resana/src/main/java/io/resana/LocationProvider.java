package io.resana;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationProvider implements LocationListener {
    private static final String TAG = "LocationProvider";
    private static final int ONE_MINUTE = 1000 * 60;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    Delegate delegate;
    private Context appContext;
    private LocationManager locationManager;
    private Location currLocation;

    LocationProvider(Context context, Delegate  delegate) {
        this.appContext = context;
        this.delegate = delegate;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    interface Delegate {
        void onLocationChanged(Location location, boolean isFinal);
    }
}
