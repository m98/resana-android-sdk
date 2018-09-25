package io.resana;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

class AdVersionKeeper {
    private static final String PREF_NAME = "RESANA_AD_VERSIONS_4387";
    private static SharedPreferences prefs;

    static void init(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        cleanupPrefs();
    }

    static void updateAdVersion(Ad ad) {
        if (ad.data.version > 0 && getAdLatestVersion(ad) < ad.data.version)
            prefs.edit()
                    .putString("v_" + ad.getOrder(), ad.data.version + "_" + System.currentTimeMillis())
                    .apply();
    }

    static void adRendered(Ad ad) {
        ad.renderedCount++; //todo take a look at here to see renderCount is working properly
        int orc = getOrderRenderCount(ad.getOrder());
        orc++;
        prefs.edit()
                .putString("r_" + ad.getOrder(), orc + "_" + System.currentTimeMillis())
                .apply();
    }

    static void adRendered(String order) {
        int orc = getOrderRenderCount(order);
        orc++;
        prefs.edit()
                .putString("r_" + order, orc + "_" + System.currentTimeMillis())
                .apply();
    }

    static boolean isOldVersion(Ad ad) {
        return ad.data.version < getAdLatestVersion(ad);
    }

    static boolean isRenderedEnough(Ad ad) {
        return getOrderRenderCount(ad.getOrder()) >= ad.data.maxView
                || ad.renderedCount >= ad.data.ctl;
    }

    private static void cleanupPrefs() {
        final Map<String, ?> map = prefs.getAll();
        final SharedPreferences.Editor editor = prefs.edit();
        long t = 5 * 24 * 60 * 60 * 1000; //5 days
        for (String k : map.keySet()) {
            if (System.currentTimeMillis() - Long.valueOf(((String) map.get(k)).split("_")[1]) > t)
                editor.remove(k);
        }
        editor.apply();
    }

    private static int getAdLatestVersion(Ad ad) {
        final String s = prefs.getString("v_" + ad.getOrder(), null);
        if (s != null)
            return Integer.valueOf(s.split("_")[0]);
        return -1;
    }

    private static int getOrderRenderCount(String orderId) {
        final String s = prefs.getString("r_" + orderId, null);
        if (s != null)
            return Integer.valueOf(s.split("_")[0]);
        return 0;
    }
}