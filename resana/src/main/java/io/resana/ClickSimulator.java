package io.resana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ClickSimulator {
    private static final String PREFS = "RESANA_CS_PREF_43897";
    private static ClickSimulator instance;
    private Context appContext;
    private ExpiringSharedPreferences prefs;
    private List<SimulateClickDto> waitingForNetwork = new ArrayList<>();
    private Set<SimulateClickDto> currentlyRunningSimulations = new HashSet<>();

    //only receive simulate clicks
    private Map<SimulateClickDto, Ad> receiveSimulateClicks = new HashMap<>();
    private Map<Ad, Integer[]> pendingAds = new HashMap<>(); // Map<Ad , [#pendingRequests, status]   status::  0:success

    private ClickSimulator(Context context) {
        this.appContext = context;
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Util.isConnectedToInternet(context)) {
                    final List<SimulateClickDto> scs = waitingForNetwork;
                    waitingForNetwork = new ArrayList<>();
                    for (SimulateClickDto sc : scs)
                        runSimulateClick(sc, false);
                }
            }
        }, new IntentFilter(new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)));
        prefs = new ExpiringSharedPreferences(appContext, PREFS, 30 * 60 * 1000); //expires after 30 mins
    }

    static ClickSimulator getInstance(Context context) {
        ClickSimulator localInstance = instance;
        if (localInstance == null) {
            synchronized (ClickSimulator.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new ClickSimulator(context);
                }
            }
        }
        return localInstance;
    }

    void persistSimulateClicks(String id, SimulateClickDto[] simulateClicks) {
        prefs.edit()
                .putString(id, DtoParser.toString(simulateClicks), 10 * 60 * 60 * 1000) //10 hours
                .apply();
    }

    void checkSimulateClicks(String id, int when) {
        final String s = prefs.getString(id, null);
        if (TextUtils.isEmpty(s))
            return;
        SimulateClickDto[] scs = null;
        try {
            scs = DtoParser.parse(s, SimulateClickDto[].class);
        } catch (Exception ignored) {
        }
        if (scs == null || scs.length == 0)
            return;
        for (SimulateClickDto sc : scs) {
            if (sc.when == when)
                runSimulateClick(sc, true);
        }
        //we may want to retry failed clicks ?
    }

    void checkSimulateClicks(Ad ad, int when) {
        if (ad.data.simulateClicks != null) {
            for (SimulateClickDto sc : ad.data.simulateClicks)
                if (sc.when == when)
                    runSimulateClick(sc, true);
            //we may want to retry failed clicks ?
        }
    }

    void runOnReceiveSimulateClicksAndNotifySuccess(Ad ad) {
        List<SimulateClickDto> scs = new ArrayList<>();
        if (ad.data.simulateClicks != null)
            for (SimulateClickDto sc : ad.data.simulateClicks)
                if (sc.when == SimulateClickDto.ON_RECEIVE)
                    scs.add(sc);
        if (scs.isEmpty()) {
            ResanaInternal resana = ResanaInternal.instance;
            //todo check this later
//            if (resana != null)
//                resana.onReceiveSimulateClicksDone(ad);
        } else {
            for (SimulateClickDto sc : scs)
                receiveSimulateClicks.put(sc, ad);
            pendingAds.put(ad, new Integer[]{scs.size(), 0});
            for (SimulateClickDto sc : scs)
                new RunSimulateClick(appContext, sc).executeOnExecutor(AdViewUtil.getCommonExecutor());
        }
    }

    private void requestDone(SimulateClickDto sc, boolean success) {
        Ad ad = receiveSimulateClicks.remove(sc);
        if (ad != null) {
            Integer[] params = pendingAds.get(ad);
            params[0]--; //decrease number of pending requests
            if (!success)
                params[1] = 1; //failed
            if (params[0] == 0) {
                pendingAds.remove(ad);
                if (params[1] == 0) { // all request were successful
                    ResanaInternal resana = ResanaInternal.instance;
                    //todo check this later
//                    if (resana != null)
//                        resana.onReceiveSimulateClicksDone(ad);
                }
            }
        } else {
            currentlyRunningSimulations.remove(sc);
            prefs.edit().putString("status" + sc.hashCode(), success ? "success" : "failure", 60 * 60 * 1000).apply();
        }
    }

    private void runSimulateClick(SimulateClickDto sc, boolean checkNetwork) {
        if ("success".equals(prefs.getString("status" + sc.hashCode(), null)))
            return;
        if (currentlyRunningSimulations.contains(sc))
            return;
        if (checkNetwork && !Util.isConnectedToInternet(appContext)) {
            waitingForNetwork.add(sc);
        } else {
            currentlyRunningSimulations.add(sc);
            new RunSimulateClick(appContext, sc).executeOnExecutor(AdViewUtil.getCommonExecutor());
        }
    }

    /**
     * a thread for sending click requests to server
     */
    private static class RunSimulateClick extends AsyncTask<Void, Void, Boolean> {
        Context appContext;
        SimulateClickDto simulateClick;
        boolean networkUnavailable;

        RunSimulateClick(Context context, SimulateClickDto simulateClick) {
            this.appContext = context.getApplicationContext();
            this.simulateClick = simulateClick;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            networkUnavailable = !Util.isConnectedToInternet(appContext);
        }

        @Override
        protected Boolean doInBackground(Void... p) {
            if (networkUnavailable)
                return false;
            HttpURLConnection connection = null;
            try {
                connection = NetworkHelper.openConnection(simulateClick.method, simulateClick.url, simulateClick.headers, simulateClick.params);
                return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            } catch (NetworkHelper.UrlNotSupportedException e) {
                //we got a non http url by following redirects on original url
                //it is mainly because of market and bazzar url schemes so we assume it a successful click
                return true;
            } catch (Exception ignored) {
            } finally {
                if (connection != null)
                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {
                    }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ClickSimulator.getInstance(appContext).requestDone(simulateClick, success);
        }
    }
}