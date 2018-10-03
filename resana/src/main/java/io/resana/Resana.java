package io.resana;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Resana {
    ResanaInternal instance;

    private static Set<Resana> references = Collections.synchronizedSet(new HashSet<Resana>());

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

    public static Resana create(Context context, String[] tags, int logLevel, ResanaConfig resanaConfig) {
        if (resanaConfig == null)
            throw new IllegalArgumentException("ResanaConfig cannot be null");
        ResanaConfig.saveConfigs(context, resanaConfig);
        ResanaLog.setLogLevel(logLevel);
        final Resana resana = new Resana(ResanaInternal.getInstance(context, tags));
        references.add(resana);
        return resana;
    }

    public static Resana create(Context context, String[] tags, ResanaConfig resanaConfig) {
        return create(context, tags, LOG_LEVEL_VERBOSE, resanaConfig);
    }

    public static Resana create(Context context, ResanaConfig resanaConfig) {
        return create(context, null, LOG_LEVEL_VERBOSE, resanaConfig);
    }

    public static Resana create(Context context, int logLevel, ResanaConfig resanaConfig) {
        return create(context, null, logLevel, resanaConfig);
    }

    public void release() {
        if (references.remove(this)) {
            if (references.size() == 0)
                instance.internalRelease();
            instance = null;
        }
    }

    public NativeAd getNativeAd(boolean hasTitle) {
        checkInstance();
        return instance.getNativeAd(hasTitle);
    }

    public NativeAd getNativeAd(boolean hasTitle, String zone) {
        checkInstance();
        return instance.getNativeAd(hasTitle, zone);
    }

    public void onNativeAdRendered(NativeAd ad) {
        checkInstance();
        instance.onNativeAdRendered(ad);
    }

    public void onNativeAdClicked(Context context, NativeAd ad) {
        checkInstance();
        instance.onNativeAdClicked(context, ad);
    }

    public void onAdDismissed(String secretKey, DismissOption reason) {
        checkInstance();
        instance.onAdDismissed(secretKey, reason);
    }

    public void onNativeAdLongClick(Context context, NativeAd ad) {
        checkInstance();
        instance.onNativeAdLongClick(context, ad);
    }

    public boolean canDismissAds() {
        checkInstance();
        return instance.adsAreDismissible;
    }

    public List<DismissOption> getDismissOptions() {
        checkInstance();
        if (instance.adsAreDismissible && instance.dismissOptions != null) {
            List<DismissOption> options = new ArrayList<>();
            options.addAll(instance.dismissOptions);
            return options;
        }
        return null;
    }

    private void checkInstance() {
        if (!references.contains(this))
            throw new IllegalStateException("Bad usage of Resana instance! You should not use an instance after calling release");
    }
}