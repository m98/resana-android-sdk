package io.resana;

import android.content.Context;

import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_CHANCE;
import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE;
import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL;
import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS;
import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_TS;
import static io.resana.ResanaPreferences.PREF_CTRL_NATIVE_COOL_DOWN_TTL;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_CHANCE;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_INTERVAL;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_TS;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_TS;
import static io.resana.ResanaPreferences.PREF_CTRL_SPLASH_COOL_DOWN_TTL;
import static io.resana.ResanaPreferences.getFloat;
import static io.resana.ResanaPreferences.getInt;
import static io.resana.ResanaPreferences.getLong;
import static io.resana.ResanaPreferences.remove;
import static io.resana.ResanaPreferences.saveLong;

class CoolDownHelper {

    static boolean shouldShowSplash(Context context) {
        validateSplashCoolDownData(context);
        if (isFirstSplash(context)) {
            saveLong(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_TS, System.currentTimeMillis());
            final float first = getFloat(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE, 1f);
            return Math.random() < first;
        }
        final float chance = getFloat(context, PREF_CTRL_SPLASH_COOL_DOWN_CHANCE, 1f);
        return Math.random() < chance;
    }

    private static boolean isFirstSplash(Context context) {
        final int interval = getInt(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_INTERVAL, -1);
        final float first = getFloat(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE, -1);
        final long lastTime = getLong(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_TS, -1);
        if (interval < 0 || first < 0)
            return false;
        return System.currentTimeMillis() > lastTime + interval * 1000;
    }


    static boolean shouldShowNativeAd(Context context) {
        float chance = Util.getNativeChance(context);
        return Math.random() <= chance;
    }

    private static boolean isFirstNativeAd(Context context) {
        final int interval = getInt(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL, -1);
        final float first = getFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE, -1);
        final long lastTime = getLong(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS, -1);
        if (interval < 0 || first < 0)
            return false;
        return System.currentTimeMillis() > lastTime + interval * 1000;
    }

    static private void validateSplashCoolDownData(Context context) {
        final long ts = getLong(context, PREF_CTRL_SPLASH_COOL_DOWN_TS, -1);
        final long ttl = getInt(context, PREF_CTRL_SPLASH_COOL_DOWN_TTL, -1);
        if (ts < 0 || ttl < 0)
            return;
        if (System.currentTimeMillis() < ts + ttl * 1000)
            return;
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_CHANCE);
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_TS);
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_TTL);
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE);
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_TS);
        remove(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_INTERVAL);
    }

    static private void validateNativeAdCoolDownData(Context context) {
        final long ts = getLong(context, PREF_CTRL_NATIVE_COOL_DOWN_TS, -1);
        final long ttl = getInt(context, PREF_CTRL_NATIVE_COOL_DOWN_TTL, -1);
        if (ts < 0 || ttl < 0)
            return;
        if (System.currentTimeMillis() < ts + ttl * 1000)
            return;
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_CHANCE);
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_TS);
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_TTL);
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE);
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS);
        remove(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL);
    }
}
