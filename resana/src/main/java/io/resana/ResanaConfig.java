package io.resana;

import android.content.Context;

import static io.resana.ResanaPreferences.*;

public class ResanaConfig {
    public enum AdType {
        SPLASH, NATIVE
    }

    public enum VisualType {
        SQUARE, HORIZONTAL, ORIGINAL
    }

    AdType[] adTypes;
    VisualType[] visualTypes;

    public ResanaConfig(AdType[] adTypes, VisualType[] visualTypes) {
        this.adTypes = adTypes;
        this.visualTypes = visualTypes;
    }


    static void saveConfigs(Context context, ResanaConfig resanaConfig) {
        remove(context, PREF_NATIVE_AD_TYPE);
        remove(context, PREF_SPLASH_AD_TYPE);
        remove(context, PREF_ORG_VISUAL_TYPE);
        remove(context, PREF_HRZ_VISUAL_TYPE);
        remove(context, PREF_SQ_VISUAL_TYPE);
        if (resanaConfig == null)
            return;
        AdType[] adTypes = resanaConfig.adTypes;
        VisualType[] visualTypes = resanaConfig.visualTypes;
        if (adTypes != null) {
            for (AdType adtype : adTypes) {
                switch (adtype) {
                    case NATIVE:
                        saveBoolean(context, PREF_NATIVE_AD_TYPE, true);
                        break;
                    case SPLASH:
                        saveBoolean(context, PREF_SPLASH_AD_TYPE, true);
                        break;
                }
            }
        }

        if (visualTypes != null) {
            for (VisualType visualType : visualTypes) {
                switch (visualType) {
                    case SQUARE:
                        saveBoolean(context, PREF_SQ_VISUAL_TYPE, true);
                        break;
                    case ORIGINAL:
                        saveBoolean(context, PREF_ORG_VISUAL_TYPE, true);
                        break;
                    case HORIZONTAL:
                        saveBoolean(context, PREF_HRZ_VISUAL_TYPE, true);
                        break;
                }
            }
        }
    }

    VisualType[] getVisualTypes() {
        return visualTypes;
    }

    AdType[] getAdTypes() {
        return adTypes;
    }

    public void setAdTypes(AdType[] adTypes) {
        this.adTypes = adTypes;
    }

    public void setVisualTypes(VisualType[] visualTypes) {
        this.visualTypes = visualTypes;
    }

    static boolean gettingSplashAds(Context context) {
        return getBoolean(context, PREF_SPLASH_AD_TYPE, false);
    }

    static boolean gettingNativeAds(Context context) {
        return getBoolean(context, PREF_NATIVE_AD_TYPE, false);
    }

    static boolean gettingSqVisual(Context context) {
        return getBoolean(context, PREF_SQ_VISUAL_TYPE, false);
    }

    static boolean gettingHrzVisual(Context context) {
        return getBoolean(context, PREF_HRZ_VISUAL_TYPE, false);
    }

    static boolean gettingOrgVisual(Context context) {
        return getBoolean(context, PREF_ORG_VISUAL_TYPE, false);
    }
}
