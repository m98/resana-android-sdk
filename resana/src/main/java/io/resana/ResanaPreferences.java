package io.resana;

import android.content.Context;
import android.content.SharedPreferences;

class ResanaPreferences {
    static final String PREFS = "RESANA_PREFS";

    static final String PREF_SPLASH_AD_TYPE = "PREF_SPLASH_TYPE";
    static final String PREF_NATIVE_AD_TYPE = "PREF_NATIVE_TYPE";
    static final String PREF_SQ_VISUAL_TYPE = "PREF_SQ_VISUAL_TYPE";
    static final String PREF_HRZ_VISUAL_TYPE = "PREF_HRZ_VISUAL_TYPE";
    static final String PREF_ORG_VISUAL_TYPE = "PREF_ORG_VISUAL_TYPE";

    static final String PREF_DOWNLOADING_APK = "PREF_DOWNLOADING_APK";


    static final String PREF_LOG_LEVEL = "PREF_LOG_LEVEL";
    static final String PREF_CONTINUOUS_CLOSES = "PREF_CONTINUOUS_CLOSES";
    static final String PREF_CONTINUOUS_CLOSES_TYPES = "PREF_CONTINUOUS_CLOSES_TYPES";
    static final String PREF_LAST_SUCCESSFUL_CONNECT_TIME = "PREF_LAST_SUCCESSFUL_CONNECT_TIME";
    static final String PREF_CONNECT_ANOMALY_DATA_RECORDING_TIME = "PREF_CONNECT_ANOMALY_DATA_RECORDING_TIME";
    static final String PREF_LAST_RECEIVED_MESSAGES = "PREF_LAST_RECEIVED_MESSAGES";
    static final String PREF_PENDING_RESANA_ACKS = "PREF_PENDING_RESANA_ACKS";
    static final String PREF_MEDIA_ID = "MEDIA_ID";
    static final String PREF_CATEGORIES = "CATEGORIES";
    static final String PREF_LOC_LAT = "PREF_LOC_LAT";
    static final String PREF_LOC_LONG = "PREF_LOC_LONG";
    static final String PREF_LOC_ACU = "PREF+_LOC_ACU";
    static final String PREF_LOC_TIME = "PREF_LOC_TIME";


    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_MAX = "CTRL_SUB_CD_MAX";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_MIN = "CTRL_SUB_CD_MIN";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST = "CTRL_SUB_CD_FIRST";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_TTL = "CTRL_SUB_CD_TTL";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_TS = "CTRL_SUB_CD_TS";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_CHANCE = "CTRL_SUB_CD_CHANCE";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE = "CTRL_SUB_CD_FCH";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_TS = "CTRL_SUB_CD_FCH_TS";
    static final String PREF_CTRL_SUBTITLE_COOL_DOWN_FIRST_CHANCE_INTERVAL = "CTRL_SUB_CD_FCH_I";

    static final String PREF_CTRL_SPLASH_COOL_DOWN_CHANCE = "CTRL_SP_CD_CHANCE";
    static final String PREF_CTRL_SPLASH_COOL_DOWN_TTL = "CTRL_SP_CD_TTL";
    static final String PREF_CTRL_SPLASH_COOL_DOWN_TS = "CTRL_SP_CD_TS";
    static final String PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE = "CTRL_SP_CD_FCH";
    static final String PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_TS = "CTRL_SP_CD_FCH_TS";
    static final String PREF_CTRL_SPLASH_COOL_DOWN_FIRST_CHANCE_INTERVAL = "CTRL_SP_CD_FCH_I";

    static final String PREF_CTRL_NATIVE_COOL_DOWN_CHANCE = "CTRL_NATIVE_CD_CHANCE";
    static final String PREF_CTRL_NATIVE_COOL_DOWN_TTL = "CTRL_NATIVE_CD_TTL";
    static final String PREF_CTRL_NATIVE_COOL_DOWN_TS = "CTRL_NATIVE_CD_TS";
    static final String PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE = "CTRL_NATIVE_CD_FCH";
    static final String PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_TS = "CTRL_NATIVE_CD_FCH_TS";
    static final String PREF_CTRL_NATIVE_COOL_DOWN_FIRST_CHANCE_INTERVAL = "CTRL_NATIVE_CD_FCH_I";

    static final String PREF_SHOULD_CLEANUP_OLD_FILES = "SHOULD_CLEANUP_OLD_FILES";
    static final String PREF_RESANA_INFO_TEXT = "RESANA_INFO_TEXT";
    static final String PREF_RESANA_INFO_LABEL = "RESANA_INFO_LABEL";
    static final String PREF_LAST_SESSION_START_TIME = "SESSION_START_TIME";
    static final String PREF_LAST_SESSION_DURATION = "SESSION_DURATION";
    static final String PREF_REPORT_INSTALLED_PKGS = "R_INS_PKGS";
    static final String PREF_REPORT_INSTALLED_PKGS_INTERVAL = "R_INS_PKGS_I";
    static final String PREF_DISMISS_ENABLE = "DISMISS_X";
    static final String PREF_DISMISS_OPTIONS = "DISMISS_O";
    static final String PREF_DISMISS_REST_DURATION = "DISMISS_RD";
    static final String PREF_LAST_DISMISS = "LAST_DISMISS";
    static final String PREF_REPORT_METADATA_FIELDS = "TEL_MDATA_LIST";
    static final String PREF_REPORT_METADATA_INTERVAL = "TEL_MDATA_I";
    static final String PREEF_BLOCKED_ZONES = "BLOCKED_ZONES";

    static final String PREF_VISUAL_INDEX = "PREF_VIS_INDEX";

    static final String PREF_DELETE_FILES_TIME = "L_MODIFIED_D";

    private static SharedPreferences prefs;

    static SharedPreferences getPrefs(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs;
    }

    static int getInt(Context context, String key, int defValue) {
        return getPrefs(context).getInt(key, defValue);
    }

    static void saveInt(Context context, String key, int value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    static String getString(Context context, String key, String defValue) {
        return getPrefs(context).getString(key, defValue);
    }

    static void saveString(Context context, String key, String value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    static float getFloat(Context context, String key, float defValue) {
        return getPrefs(context).getFloat(key, defValue);
    }

    static void saveFloat(Context context, String key, float value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    static boolean getBoolean(Context context, String key, boolean defValue) {
        return getPrefs(context).getBoolean(key, defValue);
    }

    static void saveBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static long getLong(Context context, String key, long defValue) {
        return getPrefs(context).getLong(key, defValue);
    }

    static void saveLong(Context context, String key, long value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putLong(key, value);
        editor.apply();
    }

    static void remove(Context context, String key) {
        getPrefs(context).edit().remove(key).apply();
    }
}