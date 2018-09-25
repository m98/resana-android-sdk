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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.List;

interface BefrestInternal {
    int LOG_LEVEL_DEFAULT = Befrest.LOG_LEVEL_INFO;

    void setStartServiceAlarm();

    String getSubscribeUri();

    List<KeyValue> getSubscribeHeaders();

    int getSendOnAuthorizeBroadcastDelay();

    void sendBefrestBroadcast(Context context, int type, Bundle extras);

    void reportOnClose(Context context, int code);

    void reportOnOpen(Context context);

    class Util {
        private static final String TAG = ResanaLog.TAG_PREF + "Util";
        protected static final String KEY_MESSAGE_PASSED = "KEY_MESSAGE_PASSED";
        private static final String BROADCAST_SENDING_PERMISSION_POSTFIX = ".permission.RESANA_ADS";
        static final int SDK_VERSION = 2;

        /**
         * Is device connected to Internet?
         */
        static boolean isConnectedToInternet(Context context) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && (netInfo.isConnectedOrConnecting() || netInfo.isAvailable())) {
                    return true;
                }

                netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                } else {
                    netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                ResanaLog.e(TAG, e);
                return true;
            }
            return false;
        }

        static String getBroadcastSendingPermission(Context context) {
            return context.getApplicationContext().getPackageName() + BROADCAST_SENDING_PERMISSION_POSTFIX;
        }

        static boolean isWifiConnected(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return mWifi.isConnected();
        }

        static String decodeBase64(String s) {
            try {
                return new String(Base64.decode(s, Base64.DEFAULT), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "";
        }

        static boolean isWakeLockPermissionGranted(Context context) {
            int res = context.checkCallingOrSelfPermission(Manifest.permission.WAKE_LOCK);
            return (res == PackageManager.PERMISSION_GRANTED);
        }
    }
}
