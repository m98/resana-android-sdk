package io.resana;

import android.content.Context;

class AdDatabase {
    private static String PREF_KEYS = "RESANA_AD_KEYS" + 1;
    private ExpiringSharedPreferences prefs;
    private static AdDatabase instance;

    private AdDatabase(Context context) {
        prefs = new ExpiringSharedPreferences(context.getApplicationContext(), PREF_KEYS, 24 * 60 * 60 * 1000);
    }

    static AdDatabase getInstance(Context context) {
        AdDatabase localInstance = instance;
        if (localInstance == null) {
            synchronized (AdDatabase.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new AdDatabase(context);
                }
            }
        }
        return localInstance;
    }

    String generateSecretKey(Ad ad) {
        String key = System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
        prefs.edit().putString(key, ad.getOrder()).apply();
        return key;
    }

    String getOrderIdForSecretKey(String secertKey) {
        return prefs.getString(secertKey, null);
    }
}
