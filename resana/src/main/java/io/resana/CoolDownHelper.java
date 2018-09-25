package io.resana;

import android.content.Context;

import static io.resana.ResanaPreferences.*;

class CoolDownHelper {
    private static final int DEFAULT_SUBTITLE_COOL_DOWN_MAX = 35;
    private static final int DEFAULT_SUBTITLE_COOL_DOWN_MIN = 15;
    private static final int DEFAULT_SUBTITLE_FIRST_WAIT_TIME = 10;

    static int getDefaultSubtitleWaitTime(boolean first) {
        if (first)
            return DEFAULT_SUBTITLE_FIRST_WAIT_TIME * 1000;
        else
            return (DEFAULT_SUBTITLE_COOL_DOWN_MAX + DEFAULT_SUBTITLE_COOL_DOWN_MIN) / 2 * 1000;
    }

    static boolean shouldShowSubtitle(Context context) {
        validateSubtitleCoolDownData(context);
        if (isFirstSubtitle(context)) {
            saveLong(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_TS, System.currentTimeMillis());
            final float first = getFloat(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE, 1f);
            return Math.random() < first;
        }
        final float chance = getPrefs(context).getFloat(PREF_CTRL_SUBTITLE_COOL_DOWN_CHANCE, 1f);
        return Math.random() < chance;
    }

    private static boolean isFirstSubtitle(Context context) {
        final int interval = getInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_INTERVAL, -1);
        final float first = getFloat(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE, -1);
        if (interval < 0 || first < 0)
            return false;
        final long lastTime = getLong(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_TS, -1);
        return System.currentTimeMillis() > lastTime + interval * 1000;
    }

    static int getSubtitleFirstWait(Context context) {
        validateSubtitleCoolDownData(context);
        final int fWait = getPrefs(context).getInt(PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST, DEFAULT_SUBTITLE_FIRST_WAIT_TIME) * 1000;
        return (int) (Math.random() * fWait * 2);
    }

    static int getSubtitleWait(Context context) {
        validateSubtitleCoolDownData(context);
        final int min = getInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MIN, DEFAULT_SUBTITLE_COOL_DOWN_MIN) * 1000;
        final int max = getInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MAX, DEFAULT_SUBTITLE_COOL_DOWN_MAX) * 1000;
        return min + (int) (Math.random() * (max - min));
    }

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
        validateNativeAdCoolDownData(context);
        if (isFirstNativeAd(context)) {
            saveLong(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS, System.currentTimeMillis());
            final float first = getFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE, 1f);
            return Math.random() < first;
        }
        final float chance = getFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_CHANCE, 1f);
        return Math.random() < chance;
    }

    private static boolean isFirstNativeAd(Context context) {
        final int interval = getInt(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL, -1);
        final float first = getFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE, -1);
        final long lastTime = getLong(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS, -1);
        if (interval < 0 || first < 0)
            return false;
        return System.currentTimeMillis() > lastTime + interval * 1000;
    }

    static private void validateSubtitleCoolDownData(Context context) {
        final long ts = getPrefs(context).getLong(PREF_CTRL_SUBTITLE_COOL_DOWN_TS, -1);
        final long ttl = getPrefs(context).getInt(PREF_CTRL_SUBTITLE_COOL_DOWN_TTL, -1);
        if (ts < 0 || ttl < 0)
            return;
        if (System.currentTimeMillis() < ts + ttl * 1000)
            return;
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MIN);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MAX);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_CHANCE);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_TS);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_TTL);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_TS);
        remove(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_INTERVAL);
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

    static void handleCoolDownCtrl(Context context, ControlDto ctrl) {
        final ControlDto.CoolDownParams.Chance splash = ((ControlDto.CoolDownParams) ctrl.params).splash;
        final ControlDto.CoolDownParams.Subtitle subtitle = ((ControlDto.CoolDownParams) ctrl.params).subtitle;
        final ControlDto.CoolDownParams.Chance nativeAd = ((ControlDto.CoolDownParams) ctrl.params).nativeAd;
        if (splash != null) {
            saveLong(context, PREF_CTRL_SPLASH_COOL_DOWN_TS, System.currentTimeMillis());
            saveFloat(context, PREF_CTRL_SPLASH_COOL_DOWN_CHANCE, (float) (splash.chance));
            saveInt(context, PREF_CTRL_SPLASH_COOL_DOWN_TTL, splash.ttl);
            saveFloat(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE, (float) splash.first);
            saveInt(context, PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_INTERVAL, splash.interval);
        }
        if (subtitle != null) {
            saveLong(context, PREF_CTRL_SUBTITLE_COOL_DOWN_TS, System.currentTimeMillis());
            saveInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MIN, subtitle.min);
            saveInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_MAX, subtitle.max);
            saveInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST, subtitle.first);
            saveFloat(context, PREF_CTRL_SUBTITLE_COOL_DOWN_CHANCE, (float) subtitle.chance);
            saveInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_TTL, subtitle.ttl);
            saveFloat(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE, (float) subtitle.firstChance);
            saveInt(context, PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_INTERVAL, subtitle.interval);
        }
        if (nativeAd != null) {
            saveLong(context, PREF_CTRL_NATIVE_COOL_DOWN_TS, System.currentTimeMillis());
            saveFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_CHANCE, (float) (nativeAd.chance));
            saveInt(context, PREF_CTRL_NATIVE_COOL_DOWN_TTL, (nativeAd.ttl));
            saveFloat(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE, (float) nativeAd.first);
            saveInt(context, PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL, nativeAd.interval);
        }
    }
}
