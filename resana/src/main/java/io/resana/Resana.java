package io.resana;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Resana {
    ResanaInternal instance;

    private static Context appContext;

    /**
     * Every Detail Will Be Printed In Logcat.
     */
    public static final int LOG_LEVEL_VERBOSE = 2;
    /**
     * Data Needed For Debug Will Be Printed.
     */
    public static final int LOG_LEVEL_DEBUG = 3;
    /**
     * Standard Level. You Will Be Aware Of BefrestImpl's Main State
     */
    public static final int LOG_LEVEL_INFO = 4;
    /**
     * Only Warning And Errors.
     */
    public static final int LOG_LEVEL_WARN = 5;
    /**
     * Only Errors.
     */
    public static final int LOG_LEVEL_ERROR = 6;
    /**
     * None Of BefrestImpl Logs Will Be Shown.
     */
    public static final int LOG_LEVEL_NO_LOG = 100;

    private Resana(ResanaInternal resanaInternal) {
        this.instance = resanaInternal;
    }

    public static void init(Context context, ResanaConfig resanaConfig) {
        if (resanaConfig == null)
            throw new IllegalArgumentException("ResanaConfig cannot be null");
        appContext = context;
        ResanaConfig.saveConfigs(appContext, resanaConfig);
        ResanaInternal.getInstance(appContext);
    }

    public static NativeAd getNativeAd(boolean hasTitle) {
        return ResanaInternal.getInstance(appContext).getNativeAd(hasTitle);
    }

    public static NativeAd getNativeAd(boolean hasTitle, String zone) {
        return ResanaInternal.getInstance(appContext).getNativeAd(hasTitle, zone);
    }

    public void onNativeAdRendered(NativeAd ad) {
        ResanaInternal.getInstance(appContext).onNativeAdRendered(ad);
    }

    public void onNativeAdClicked(Context context, NativeAd ad) {
        onNativeAdClicked(context, ad, null);
    }

    public void onNativeAdClicked(Context context, NativeAd ad, AdDelegate adDelegate) {
        ResanaInternal.getInstance(context).onNativeAdClicked(context, ad, adDelegate);
    }

    public void onAdDismissed(String secretKey, DismissOption reason) {
        ResanaInternal.getInstance(appContext).onAdDismissed(secretKey, reason);
    }

    public void onNativeAdLongClick(Context context, NativeAd ad) {
        ResanaInternal.getInstance(context).onNativeAdLongClick(context, ad);
    }

    public boolean canDismissAds() {
        return ResanaInternal.getInstance(appContext).adsAreDismissible;
    }

    public List<DismissOption> getDismissOptions() {
        if (ResanaInternal.getInstance(appContext).adsAreDismissible && ResanaInternal.getInstance(appContext).dismissOptions != null) {
            List<DismissOption> options = new ArrayList<>();
            options.addAll(instance.dismissOptions);
            return options;
        }
        return null;
    }

    public static String getVersion() {
        return ResanaInternal.SDK_VERSION;
    }
}