/******************************************************************************
 * Copyright 2015-2016 Befrest
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.resana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.resana.ResanaPreferences.PREF_CATEGORIES;
import static io.resana.ResanaPreferences.PREF_CONNECT_ANOMALY_DATA_RECORDING_TIME;
import static io.resana.ResanaPreferences.PREF_CONTINUOUS_CLOSES;
import static io.resana.ResanaPreferences.PREF_CONTINUOUS_CLOSES_TYPES;
import static io.resana.ResanaPreferences.PREF_LAST_SUCCESSFUL_CONNECT_TIME;
import static io.resana.ResanaPreferences.PREF_MEDIA_ID;
import static io.resana.ResanaPreferences.getPrefs;
import static io.resana.ResanaPreferences.saveInt;
import static io.resana.ResanaPreferences.saveLong;
import static io.resana.ResanaPreferences.saveString;

/**
 * Main class to interact with BefrestImpl service.
 */
final class BefrestImpl implements Befrest, BefrestInternal {
    private static String TAG = ResanaLog.TAG_PREF + "BefrestImpl";

    static final int START_ALARM_CODE = 676428;
    static final int KEEP_PINGING_ALARM_CODE = 676429;

    BefrestImpl(Context context) {
        this.context = context.getApplicationContext();
        pushService = AdService.class;
        final SharedPreferences prefs = getPrefs(context);
        media = prefs.getString(PREF_MEDIA_ID, null);
        cats = prefs.getString(PREF_CATEGORIES, null);
    }


    Context context;
    Class<?> pushService;

    private String media;
    private String cats;

    String subscribeUrl;

    boolean isBefrestStarted;
    //    String topics;
    boolean connectionDataChangedSinceLastStart;

    boolean refreshIsRequested = false;
    long lastAcceptedRefreshRequestTime = 0;

    long connectAnomalyDataRecordingStartTime;

    private static final int[] AuthProblemBroadcastDelay = {0, 60 * 1000, 240 * 1000, 600 * 1000};
    int prevAuthProblems = 0;

    private static final long WAIT_TIME_BEFORE_SENDING_CONNECT_ANOMLY_REPORT = 24 * 60 * 60 * 1000; // 24h

    private int reportedContinuousCloses;
    private String continuousClosesTypes;
    
    @Override
    public Befrest init(String media, String[] categories) {
        String newCats = getFormedCategoryString(categories);
        if (!media.equals(this.media) || !newCats.equals(cats)) {
            this.media = media;
            this.cats = newCats;
            clearTempData();
            saveString(context, PREF_MEDIA_ID, media);
            saveString(context, PREF_CATEGORIES, cats);
        }
        return this;
    }

    private String getFormedCategoryString(String[] categories) {
        String res = "";
        if (categories != null)
            for (String s : categories) {
                res += s.replace("-", "_") + "-";
            }
        if (res.length() > 0)
            res = res.substring(0, res.length() - 1);
        else
            res = "NOT_SPECIFIED";
        return res;
    }


    static void startService(Context context, Class pushService, String flag) {
        try {
            Intent i = new Intent(context, pushService);
            if (flag != null) {
                i.putExtra(flag, true);
            }
            context.startService(i);
        } catch (Throwable t) {
            //Start service cannot be called from background in Android 8+
            ResanaLog.e("StartService", t);
        }
    }

    /**
     * Stop push service.
     * You can call start to run the service later.
     */
    public void stop() {
        isBefrestStarted = false;
        context.stopService(new Intent(context, pushService));
        ResanaLog.i(TAG, "resana Service Stopped.");
    }

    @Override
    public void sendMessage(String msg) {
        if (!Util.isConnectedToInternet(context) || !isBefrestStarted)
            return;
        Intent i = new Intent(context, pushService);
        i.putExtra(AdService.SEND_MESSAGE, true);
        i.putExtra(AdService.KEY_MESSAGE_TO_BE_SENT, msg);
        context.startService(i);
    }

    @Override
    public void sendToServer(String ack) {
        if (!isBefrestStarted) {
            //should not come here
            return;
        }
        Intent i = new Intent(context, pushService);
        i.putExtra(AdService.SEND_RESANA_ACK, true);
        i.putExtra(AdService.KEY_MESSAGE_TO_BE_SENT, ack);
        context.startService(i);
    }

    /**
     * Start push service. You should set uId and chId before calling this start.
     * Best Practice: call this method in your onCreate() method of your Application class
     * Yous should also call this method anytime you set authToken.
     *
     * @throws IllegalStateException if be called without a prior call to init()
     */
    public void start() {
        ResanaLog.i(TAG, "starting resana");
        isBefrestStarted = true;
        if (connectionDataChangedSinceLastStart)
            context.stopService(new Intent(context, pushService));
        BefrestImpl.startService(context, pushService, AdService.CONNECT);
        connectionDataChangedSinceLastStart = false;
    }

    /**
     * Register a new push receiver. Any registered receiver <i><b>must be</b></i> unregistered
     * by passing the same receiver object to {@link #unregisterPushReceiver}. Actually the method
     * registers a BroadcastReceiver considering security using permissions.
     *
     * @param receiver the receiver object that will receive events
     */
    public void registerPushReceiver(BefrestPushReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BefrestPushReceiver.ACTION_BEFREST_PUSH);
        context.registerReceiver(receiver, intentFilter, Util.getBroadcastSendingPermission(context), null);
    }

    /**
     * Unregister a previously registered push receiver.
     *
     * @param receiver receiver object to be unregistered
     */
    public void unregisterPushReceiver(BefrestPushReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            throw new BefrestException(e);
        }
    }

    public void setStartServiceAlarm() {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, pushService).putExtra(AdService.SERVICE_STOPPED, true);
        PendingIntent pi = PendingIntent.getService(context, START_ALARM_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = SystemClock.elapsedRealtime() + AdService.START_SERVICE_AFTER_ILLEGAL_STOP_DELAY;
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pi);
        ResanaLog.d(TAG, "resana Scheduled To Start Service In " + AdService.START_SERVICE_AFTER_ILLEGAL_STOP_DELAY + "ms");
    }

    private void clearTempData() {
        subscribeUrl = null;
        connectionDataChangedSinceLastStart = true;
    }

    public String getSubscribeUri() {
        if (subscribeUrl == null)
            subscribeUrl = "wss://gw2.resana.io/xapi/2/subscribe/" + media;
        ResanaLog.d(TAG, "subscribeUrl: " + subscribeUrl);
        return subscribeUrl;
    }

    public List<KeyValue> getSubscribeHeaders() {
        List<KeyValue> res = new ArrayList<>();
        res.add(new KeyValue("X-RS-SDK-VERSION", ResanaInternal.SDK_VERSION));
        res.add(new KeyValue("X-RS-SDK-BUILD-NUM", "" + ResanaInternal.SDK_VERSION_NUM));
        res.add(new KeyValue("X-RS-DEVICE_ID", DeviceCredentials.getDeviceUniqueId(context)));
        res.add(new KeyValue("X-RS-OPERATOR", DeviceCredentials.getOperatorName(context)));
        res.add(new KeyValue("X-RS-ANDROID-VERSION", "" + Build.VERSION.SDK_INT));
        res.add(new KeyValue("X-RS-DEVICE-MODEL", DeviceCredentials.getDeviceModel()));
        ResanaLog.v(TAG, "subscribeHeaders: " + Arrays.toString(res.toArray()));
        return res;
    }

    public int getSendOnAuthorizeBroadcastDelay() {
        int index = prevAuthProblems < AuthProblemBroadcastDelay.length
                ? prevAuthProblems
                : AuthProblemBroadcastDelay.length - 1;
        return AuthProblemBroadcastDelay[index];
    }

    public void sendBefrestBroadcast(Context context, int type, Bundle extras) {
        try {
            Intent intent = new Intent(BefrestPushReceiver.ACTION_BEFREST_PUSH);
            intent.putExtra(BefrestPushReceiver.BROADCAST_TYPE, type);
            if (extras != null) intent.putExtras(extras);
            String permission = BefrestImpl.Util.getBroadcastSendingPermission(context);
            long now = System.currentTimeMillis();
            intent.putExtra(BefrestPushReceiver.KEY_TIME_SENT, "" + now);
            context.getApplicationContext().sendBroadcast(intent, permission);
            ResanaLog.v(TAG, "broadcast sent::    type: " + type + "      permission:" + permission);
        } catch (Exception e) {
            ResanaLog.w(TAG, "counld not send broadcast. type: " + type, e);
        }
    }

    public void reportOnClose(Context context, int code) {
        reportedContinuousCloses++;
        ResanaLog.d(TAG, "reportOnClose :: total:" + reportedContinuousCloses + " code:" + code);
        continuousClosesTypes += code + ",";
        saveString(context, PREF_CONTINUOUS_CLOSES_TYPES, continuousClosesTypes);
        saveInt(context, PREF_CONTINUOUS_CLOSES, reportedContinuousCloses);
        if (System.currentTimeMillis() - connectAnomalyDataRecordingStartTime > WAIT_TIME_BEFORE_SENDING_CONNECT_ANOMLY_REPORT)
            if (reportedContinuousCloses > 25) {
                // ReportCrash ?
                clearAnomalyHistory();
            }
    }

    public void reportOnOpen(Context context) {
        saveLong(context, PREF_LAST_SUCCESSFUL_CONNECT_TIME, System.currentTimeMillis());
        clearAnomalyHistory();
    }

    private void clearAnomalyHistory() {
        connectAnomalyDataRecordingStartTime = System.currentTimeMillis();
        saveLong(context, PREF_CONNECT_ANOMALY_DATA_RECORDING_TIME, connectAnomalyDataRecordingStartTime);
        reportedContinuousCloses = 0;
        continuousClosesTypes = "";
        saveString(context, PREF_CONTINUOUS_CLOSES_TYPES, continuousClosesTypes);
        saveInt(context, PREF_CONTINUOUS_CLOSES, reportedContinuousCloses);
    }

    /**
     * Request the push service to refresh its connection. You will be notified through your receivers
     * whenever the connection refreshed.
     *
     * @return true if a request was accepted, false otherwise.
     */
    public boolean refresh() {
        if (!Util.isConnectedToInternet(context) || !isBefrestStarted)
            return false;
        ResanaLog.i(TAG, "resana Is Refreshing ...");
        if (refreshIsRequested && (System.currentTimeMillis() - lastAcceptedRefreshRequestTime) < 10 * 1000)
            return true;
        refreshIsRequested = true;
        lastAcceptedRefreshRequestTime = System.currentTimeMillis();
        BefrestImpl.startService(context, pushService, AdService.REFRESH);
        return true;
    }

    class BefrestException extends RuntimeException {
        public BefrestException(Throwable throwable) {
            super(throwable);
        }

        public BefrestException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public BefrestException(String detailMessage) {
            super(detailMessage);
        }

        public BefrestException() {
        }
    }
}