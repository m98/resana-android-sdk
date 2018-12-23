package io.resana;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import static io.resana.ResanaPreferences.PREF_DISMISS_ENABLE;
import static io.resana.ResanaPreferences.PREF_DISMISS_OPTIONS;
import static io.resana.ResanaPreferences.PREF_DISMISS_REST_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_DISMISS;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_START_TIME;
import static io.resana.ResanaPreferences.getLong;
import static io.resana.ResanaPreferences.getPrefs;
import static io.resana.ResanaPreferences.saveLong;

class ResanaInternal {
    private static final String TAG = ResanaLog.TAG_PREF + "Resana";
    static final String SDK_VERSION = "7.4.6";
    static final int SDK_VERSION_NUM = 18;
    static final int DB_VERSION = 8;

    static final String DEFAULT_RESANA_INFO_TEXT =
            "تبلیغات رسانا" + "\n"
                    + "اپلیکیشن نمایش‌دهنده هیچ مسئولیتی در قبال محتوای این تبلیغات ندارد." + "\n"
                    + "اطلاعات بیشتر در:" + "\n"
                    + "www.resana.io" + "\n"
                    + "تماس با ما:" + "\n"
                    + "info@resana.io";

    static ResanaInternal instance;

    private final Context appContext;
    static String mediaId;
    static String deviceId;

    private SplashAdProvider splashProvider;

    boolean adsAreDismissible;
    List<DismissOption> dismissOptions;
    private long lastDismissTime;
    private int dismissRestDuration;

    private ResanaInternal(Context context) {
        ResanaLog.v(TAG, "Starting Resana");
        this.appContext = context.getApplicationContext();
        loadDismissOptions();
        GoalActionMeter.getInstance(appContext);
        AdVersionKeeper.init(appContext);
        ApkManager.getInstance(appContext);
        mediaId = AdViewUtil.getMediaId(appContext);
        if (mediaId == null)
            throw new IllegalArgumentException("ResanaMediaId is not defined properly");
        deviceId = DeviceCredentials.getDeviceUniqueId(appContext);
        //todo handle config here
        FileManager.getInstance(appContext).cleanupOldFilesIfNeeded();
        FileManager.getInstance(appContext).deleteOldAndCorruptedFiles();
        NativeAdProvider.getInstance(appContext);
        NetworkManager.checkUserAgent(appContext);
        start();
    }

    private void start() {
        saveLong(appContext, PREF_LAST_SESSION_START_TIME, System.currentTimeMillis());
        DataCollector.reportSessionDuration(getLong(appContext, PREF_LAST_SESSION_DURATION, -1));
    }

    static ResanaInternal getInstance(Context context) {
        ResanaInternal localInstance = instance;
        if (localInstance == null) {
            synchronized (ResanaInternal.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new ResanaInternal(context);
                }
            }
        }
        return localInstance;
    }

    void internalRelease() {
        saveSessionDuration();
        instance = null;
    }

    NativeAd getNativeAd(boolean hasTitle) {
        return getNativeAd(hasTitle, "");
    }

    NativeAd getNativeAd(boolean hasTitle, String zone) {
        if (!ResanaConfig.gettingNativeAds(appContext)) {
            ResanaLog.e(TAG, "You didn't mention native ads in resana config");
            return null;
        }
        return NativeAdProvider.getInstance(appContext).getAd(hasTitle, zone);
    }

    void attachSplashViewer(SplashAdView adView) {
        if (!ResanaConfig.gettingSplashAds(appContext)) {
            ResanaLog.e(TAG, "You didn't mention splash ads in resana config");
            return;
        }
        splashProvider.attachViewer(adView);
    }

    void releaseSplash(Ad ad) {
        if (splashProvider == null)
            return;
        splashProvider.releaseAd(ad);
    }

    void detachSplashViewer(SplashAdView adView) {
        if (splashProvider == null)
            return;
        splashProvider.detachViewer(adView);
    }

    boolean isSplashAvailable() {
        if (splashProvider == null)
            return false;
        return splashProvider.isAdAvailable();
    }

    private void loadDismissOptions() {
        adsAreDismissible = getPrefs(appContext).getBoolean(PREF_DISMISS_ENABLE, false);
        dismissRestDuration = getPrefs(appContext).getInt(PREF_DISMISS_REST_DURATION, 0);
        lastDismissTime = getPrefs(appContext).getLong(PREF_LAST_DISMISS, 0);
        String s = getPrefs(appContext).getString(PREF_DISMISS_OPTIONS, null);
        if (s != null) {
            dismissOptions = new Gson().fromJson(s, new TypeToken<ArrayList<DismissOption>>() {
            }.getType());
        }
    }

    private void saveSessionDuration() {
        final long start = getLong(appContext, PREF_LAST_SESSION_START_TIME, -1);
        if (start > 0) {
            final long d = System.currentTimeMillis() - start;
            saveLong(appContext, PREF_LAST_SESSION_DURATION, d);
        }
    }

    void onNativeAdRendered(NativeAd ad) {
        NativeAdProvider.getInstance(appContext).onNativeAdRendered(ad);
    }

    void onSplashRendered(Ad ad) {
        if (splashProvider == null)
            return;
//        sendToServer(splashProvider.getRenderAck(ad));
    }

    void onNativeAdClicked(Context context, NativeAd ad, AdDelegate delegate) {
        NativeAdProvider.getInstance(appContext).onNativeAdClicked(context, ad, delegate);
//        GoalActionMeter.getInstance(appContext).checkReport(ad.getSecretKey());
    }

    void onSplashClicked(Ad ad) {
        if (splashProvider == null)
            return;
//        GoalActionMeter.getInstance(appContext).checkReport(ad.data.report);
//        sendToServer(splashProvider.getClickAck(ad));
    }

    void onNativeAdLongClick(Context context, NativeAd ad) {
        if (adsAreDismissible)
            NativeAdProvider.getInstance(appContext).showDismissOptions(context, ad, dismissOptions, instance);
    }

    void onSplashLandingClicked(Ad ad) {
        if (splashProvider == null)
            return;
//        sendToServer(splashProvider.getLandingClickAck(ad));
    }

    void onAdDismissed(String secretKey, DismissOption reason) {
        if (adsAreDismissible) {
            lastDismissTime = System.currentTimeMillis();
            getPrefs(appContext).edit().putLong(PREF_LAST_DISMISS, System.currentTimeMillis()).apply();
            String ad = AdDatabase.getInstance(appContext).getOrderIdForSecretKey(secretKey);
            if (ad != null)
                DataCollector.reportAdDismissed(ad, reason);
        }
    }

    boolean isInDismissRestTime() {
        return System.currentTimeMillis() < lastDismissTime + dismissRestDuration * 1000;
    }

    private static class GetControlDataDelegate extends FileManager.Delegate {

        @Override
        void onFinish(boolean success, Object... args) {
            if (success) {
                //todo save controls
            }
        }
    }
}