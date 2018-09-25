package io.resana;

import android.content.Context;
import android.util.Base64;

class DataCollector {
    private static final int PROPERTIES_DEFAULT_EXPIRATION = 2 * 24 * 60 * 60;
    private static String PREFS_NAME = "Resana_DataCollector_2334";
    private static ExpiringSharedPreferences propertiesPrefs;

    static void clearUserApkProperty(Context context) {
        getPropertiesPrefs(context).edit().remove("R_P_pkgs:").apply();
    }

    static void clearUserMetadataProperty(Context context) {
        getPropertiesPrefs(context).edit().remove("R_P_tuser:").apply();
    }

    static void reportSessionDuration(long val) {
        if (val > 0) {
            String report = "R_SD_" + val;
            sendToServer(report);
        }
    }

    static void reportAdDismissed(String orderId, DismissOption reason) {
        String report = "R_DA_ad:" + orderId;
        if (reason != null)
            report += "_r:" + reason.getkey();
        sendToServer(report);
    }

    private static boolean sendToServer(String s) {
        final ResanaInternal resana = ResanaInternal.instance;
        if (resana == null)
            return false;
        resana.sendToServer(s);
        return true;
    }

    private static ExpiringSharedPreferences getPropertiesPrefs(Context context) {
        if (propertiesPrefs == null)
            propertiesPrefs = new ExpiringSharedPreferences(context, PREFS_NAME, PROPERTIES_DEFAULT_EXPIRATION * 1000); // 2 days
        return propertiesPrefs;
    }
}