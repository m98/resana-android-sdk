package io.resana;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;

import java.io.UnsupportedEncodingException;

import static io.resana.ResanaPreferences.PREF_BLOCKED_ZONES;
import static io.resana.ResanaPreferences.PREF_CONTROLS_TTL;
import static io.resana.ResanaPreferences.PREF_RESANA_INFO_TEXT;
import static io.resana.ResanaPreferences.getInt;
import static io.resana.ResanaPreferences.saveInt;
import static io.resana.ResanaPreferences.saveString;

class Util {
    private static final String TAG = ResanaLog.TAG_PREF + "Util";

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

    static String decodeBase64(String s) {
        try {
            return new String(Base64.decode(s, Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    static void saveControls(Context context, ControlDto controlDto) {
        saveInt(context, PREF_CONTROLS_TTL, controlDto.controlsTTL);
        saveString(context, PREF_RESANA_INFO_TEXT, controlDto.resanaLabel);
        StringBuilder blockedZones = new StringBuilder();
        for (String blockZone : controlDto.blockedZones) {
            blockedZones.append(blockZone).append(";");
        }
        saveString(context, PREF_BLOCKED_ZONES, blockedZones.toString());
    }

    static int getControlsTTL(Context context) {
        return getInt(context, PREF_CONTROLS_TTL, 1000);
    }

    static String[] getBlockedZones(Context context) {
        String[] blockedZones;
        String s = ResanaPreferences.getString(context, PREF_BLOCKED_ZONES, null);
        if (s != null && s.length() > 0) {
            String[] zones = s.split(";");
            blockedZones = new String[zones.length];
            for (int i = 0; i < zones.length; i++) {
                blockedZones[i] = zones[i];
                ResanaLog.d(TAG, "blockedZone: " + blockedZones[i]);
            }
            return blockedZones;
        } else {
            ResanaLog.d(TAG, "loadBlockedZones: there is no block zone");
            return null;
        }
    }
}
