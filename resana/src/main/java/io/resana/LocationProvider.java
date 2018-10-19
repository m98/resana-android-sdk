package io.resana;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

public class LocationProvider implements LocationListener {
    private static final String TAG = "LocationProvider";
    private static final int ONE_MINUTE = 1000 * 60;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    Delegate delegate;
    private Context appContext;
    private LocationManager locationManager;
    private Location currLocation;

    private Handler handler = new Handler();

    private WeakRunnable<LocationProvider> startLocating = new StartLocating(this);

    private WeakRunnable<LocationProvider> finishLocating = new FinishLocating(this);

    LocationProvider(Context context, Delegate  delegate) {
        this.appContext = context;
        this.delegate = delegate;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    void start() {
        startLocationIfPossible();
    }

    void stop() {
        delegate = null;
        finishLocating();
        handler.removeCallbacks(finishLocating);
        handler.removeCallbacks(startLocating);
    }

    private void startLocationIfPossible() {
        if (canGetLocation())
            startLocating();
        else
            handler.postDelayed(startLocating, TWO_MINUTES);
    }

    private void startLocating() {
        currLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 500, this);
    }

    private void finishLocating() {
        ResanaLog.d(TAG, "finishCheckingLocation: ");
        if (locationManager != null)
            locationManager.removeUpdates(this);
        locationManager = null;
    }

    private boolean canGetLocation() {
        return hasLocationPermission() && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean hasLocationPermission() {
        return appContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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

    private static class StartLocating extends WeakRunnable<LocationProvider> {

        StartLocating(LocationProvider ref) {
            super(ref);
        }

        @Override
        void run(LocationProvider object) {
            object.startLocationIfPossible();
        }
    }

    private static class FinishLocating extends WeakRunnable<LocationProvider> {
        FinishLocating(LocationProvider ref) {
            super(ref);
        }

        @Override
        void run(LocationProvider object) {
            object.finishLocating();
        }
    }
}
