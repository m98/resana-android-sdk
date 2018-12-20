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
    private final String media;

    private NativeAdProvider nativeProvider;
    private SplashAdProvider splashProvider;

    boolean adsAreDismissible;
    List<DismissOption> dismissOptions;
    private long lastDismissTime;
    private int dismissRestDuration;

    private ResanaInternal(Context context) {
        this.appContext = context.getApplicationContext();
        loadDismissOptions();
        GoalActionMeter.getInstance(context);
        AdVersionKeeper.init(context);
        ApkManager.getInstance(context);
        media = AdViewUtil.getMediaId(appContext);
        if (media == null)
            throw new IllegalArgumentException("ResanaMediaId is not defined properly");
        //todo handle config here
        FileManager.getInstance(appContext).cleanupOldFilesIfNeeded();
        FileManager.getInstance(appContext).deleteOldAndCorruptedFiles();
        NativeAdProvider.getInstance(context);
        NetworkManager.checkUserAgent(context);
        start();
    }

    private void start() {
        ResanaLog.v(TAG, "Start");
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
        return nativeProvider.getAd(hasTitle, zone);
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
        if (nativeProvider == null || ad == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_ACK);
//        sendToServer(nativeProvider.getRenderAck(ad.getSecretKey()));
        AdVersionKeeper.adRendered(ad.getId() + "");
    }

    void onSplashRendered(Ad ad) {
        if (splashProvider == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_ACK);
        sendToServer(splashProvider.getRenderAck(ad));
    }

    void onNativeAdClicked(Context context, NativeAd ad) {
        onNativeAdClicked(context, ad, null);
    }

    void onNativeAdClicked(Context context, NativeAd ad, AdDelegate adDelegate) {
        if (nativeProvider == null || ad == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_CLICK);
        GoalActionMeter.getInstance(appContext).checkReport(ad.getSecretKey());
//        sendToServer(nativeProvider.getClickAck(ad.getSecretKey()));
        if (ad.hasLanding()) {
            nativeProvider.showLanding(context, ad, adDelegate);
        } else {
            nativeProvider.handleLandingClick(context, ad, adDelegate);
        }

    }

    void onSplashClicked(Ad ad) {
        if (splashProvider == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_CLICK);
        GoalActionMeter.getInstance(appContext).checkReport(ad.data.report);
        sendToServer(splashProvider.getClickAck(ad));
    }

    void onNativeAdLandingClicked(NativeAd ad) {
        if (nativeProvider == null || ad == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_LANDING_CLICK);
//        sendToServer(nativeProvider.getLandingAck(ad.getSecretKey()));
    }

    void onNativeAdLongClick(Context context, NativeAd ad) {
        if (nativeProvider == null || ad == null)
            return;
        if (adsAreDismissible)
            nativeProvider.showDismissOptions(context, ad, dismissOptions, instance);
    }

    void onSplashLandingClicked(Ad ad) {
        if (splashProvider == null)
            return;
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_LANDING_CLICK);
        sendToServer(splashProvider.getLandingClickAck(ad));
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

    void sendToServer(String msg) {
        //todo should be completed
    }
}