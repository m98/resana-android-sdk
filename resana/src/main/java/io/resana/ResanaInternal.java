package io.resana;

import android.content.Context;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.resana.FileManager.FileSpec;
import static io.resana.ResanaPreferences.PREF_DELETE_FILES_TIME;
import static io.resana.ResanaPreferences.PREF_DISMISS_ENABLE;
import static io.resana.ResanaPreferences.PREF_DISMISS_OPTIONS;
import static io.resana.ResanaPreferences.PREF_DISMISS_REST_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_DISMISS;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_START_TIME;
import static io.resana.ResanaPreferences.PREF_REPORT_INSTALLED_PKGS;
import static io.resana.ResanaPreferences.PREF_REPORT_INSTALLED_PKGS_INTERVAL;
import static io.resana.ResanaPreferences.PREF_REPORT_METADATA_FIELDS;
import static io.resana.ResanaPreferences.PREF_REPORT_METADATA_INTERVAL;
import static io.resana.ResanaPreferences.PREF_RESANA_INFO_LABEL;
import static io.resana.ResanaPreferences.PREF_RESANA_INFO_TEXT;
import static io.resana.ResanaPreferences.getBoolean;
import static io.resana.ResanaPreferences.getInt;
import static io.resana.ResanaPreferences.getLong;
import static io.resana.ResanaPreferences.getPrefs;
import static io.resana.ResanaPreferences.saveBoolean;
import static io.resana.ResanaPreferences.saveInt;
import static io.resana.ResanaPreferences.saveLong;

class ResanaInternal {
    private static final String TAG = ResanaLog.TAG_PREF + "Resana";
    static final String SDK_VERSION = "6.3.1";
    static final int SDK_VERSION_NUM = 16;
    static final int DB_VERSION = 6;

    static final String DEFAULT_RESANA_INFO_TEXT =
            "تبلیغات رسانا" + "\n"
                    + "اپلیکیشن نمایش‌دهنده هیچ مسئولیتی در قبال محتوای این تبلیغات ندارد." + "\n"
                    + "اطلاعات بیشتر در:" + "\n"
                    + "www.resana.io" + "\n"
                    + "تماس با ما:" + "\n"
                    + "info@resana.io";

    static ResanaInternal instance;

    private final String[] tags;
    private final Context appContext;
    private final String media;
    private AdReceiver adReceiver;

    private NativeAdProvider nativeProvider;
    private SplashAdProvider splashProvider;
    private SubtitleAdProvider subtitleProvider;

    boolean adsAreDismissible;
    List<DismissOption> dismissOptions;
    private long lastDismissTime;
    private int dismissRestDuration;

    static ResanaInternal getInstance(Context context, String[] tags) {
        ResanaInternal localInstance = instance;
        if (localInstance == null) {
            synchronized (ResanaInternal.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new ResanaInternal(context, tags);
                }
            }
        }
        return localInstance;
    }

    void internalRelease() {
        Befrest befrest = BefrestFactory.getInstance(appContext);
        try {
            befrest.unregisterPushReceiver(adReceiver);
        } catch (Exception e) {
            ResanaLog.w(TAG, "problem in unRegistering adReceiver. maybe you are calling resana.release() more that once?");
        }
        befrest.stop();
        saveSessionDuration();
        instance = null;
    }

    NativeAd getNativeAd(boolean hasTitle) {
        final NativeAd ad = nativeProvider.getAd(hasTitle, "");
        return ad;
    }

    NativeAd getNativeAd(boolean hasTitle, String zone) {
        final NativeAd ad = nativeProvider.getAd(hasTitle, zone);
        return ad;
    }

    void attachSplashViewer(SplashAdView adView) {
        splashProvider.attachViewer(adView);
    }

    void releaseSplash(Ad ad) {
        splashProvider.releaseAd(ad);
    }

    void releaseSubtitle(Ad ad) {
        subtitleProvider.releaseAd(ad);
    }

    void detachSplashViewer(SplashAdView adView) {
        splashProvider.detachViewer(adView);
    }

    boolean isSplashAvailable() {
        return splashProvider.isAdAvailable();
    }

    void attachSubtitleViewer(SubtitleAdView adView) {
        subtitleProvider.attachAdViewer(adView);
    }

    Ad getSubtitleAd(List<String> notThese, long remainedTime) {
        final Ad ad = subtitleProvider.getAd(notThese, remainedTime);
        if (ad != null)
            onSubtitleRendered(ad);
        return ad;
    }

    int getSubtitleWaitTime(boolean firstTime) {
        if (firstTime)
            return CoolDownHelper.getSubtitleFirstWait(appContext);
        return CoolDownHelper.getSubtitleWait(appContext);
    }

    private ResanaInternal(Context context, String[] tags) {
        this.appContext = context.getApplicationContext();
        this.tags = tags;
        loadDismissOptions();
        GoalActionMeter.getInstance(context);
        AdVersionKeeper.init(context);
        ApkManager.getInstance(context);
        media = AdViewUtil.getMediaId(appContext);
        if (media == null)
            throw new IllegalArgumentException("ResanaMediaId is not defined properly");
        nativeProvider = new NativeAdProvider(context);
        splashProvider = new SplashAdProvider(context);
        subtitleProvider = new SubtitleAdProvider(context);
        FileManager.getInstance(appContext).cleanupOldFilesIfNeeded();
        FileManager.getInstance(appContext).deleteOldFiles();
        NetworkHelper.checkUserAgent(appContext);
        start();
    }

    private void start() {
        ResanaLog.v(TAG, "Start");
        adReceiver = new AdReceiver();
        Befrest befrest = BefrestFactory.getInstance(appContext);
        befrest.registerPushReceiver(adReceiver);
        befrest.init(media, tags).start();
        saveLong(appContext, PREF_LAST_SESSION_START_TIME, System.currentTimeMillis());
        DataCollector.reportSessionDuration(getLong(appContext, PREF_LAST_SESSION_DURATION, -1));
    }

    private void handleControlMessage(Ad msg) {
        for (ControlDto ctrl : msg.ctrls)
            if (ControlDto.CMD_FLUSH.equals(ctrl.cmd)) {
                splashProvider.flushCache();
                subtitleProvider.flushCache();
                nativeProvider.flushCache();
            } else if (ControlDto.CMD_COOL_DOWN.equals(ctrl.cmd)) {
                CoolDownHelper.handleCoolDownCtrl(appContext, ctrl);
            } else if (ControlDto.CMD_RESANA_LABEL.equals(ctrl.cmd)) {
                handleResanaLabelCtrl(ctrl);
            } else if (ControlDto.CMD_DISMISS_OPTIONS.equals(ctrl.cmd)) {
                handleDismissOptionsCtrl(ctrl);
            } else if (ControlDto.CMD_BLOCKED_ZONES.equals(ctrl.cmd)) {
                nativeProvider.handleBlockedZones(ctrl);
            } else if (ControlDto.CMD_LAST_MODIFIED_DATE.equals(ctrl.cmd)) {
                saveLastModifiedDate(ctrl);
            }
    }

    private void handleResanaLabelCtrl(ControlDto ctrl) {
        final ControlDto.ResanaLabelParams params = (ControlDto.ResanaLabelParams) ctrl.params;
        ResanaPreferences.saveString(appContext, PREF_RESANA_INFO_TEXT, params.text);
        final String curLabel = ResanaPreferences.getString(appContext, PREF_RESANA_INFO_LABEL, "none");
        if (!curLabel.equals(params.label)) {
            ResanaPreferences.saveString(appContext, PREF_RESANA_INFO_LABEL, params.label);
            final FileManager fm = FileManager.getInstance(appContext);
            if ("none".equals(params.label))
                fm.deleteFile(new FileSpec("resana_label"), null);
            else
                fm.downloadFile(new FileSpec(params.label, "resana_label"), true, null);
        }
    }

    private void handleDismissOptionsCtrl(ControlDto ctrl) {
        ControlDto.DismissOptionsParams params = (ControlDto.DismissOptionsParams) ctrl.params;
        adsAreDismissible = params.dismissible;
        if (params.options == null) {
            dismissOptions = null;
        } else {
            dismissOptions = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.options.entrySet())
                dismissOptions.add(new DismissOption(entry.getKey(), entry.getValue()));
        }
        dismissRestDuration = params.restDuration;
        getPrefs(appContext).edit()
                .putBoolean(PREF_DISMISS_ENABLE, adsAreDismissible)
                .putInt(PREF_DISMISS_REST_DURATION, params.restDuration).apply();
        if (dismissOptions == null)
            getPrefs(appContext).edit().remove(PREF_DISMISS_OPTIONS).apply();
        else
            getPrefs(appContext).edit().putString(PREF_DISMISS_OPTIONS, new Gson().toJson(dismissOptions)).apply();

    }

    private void saveLastModifiedDate(ControlDto ctrl) {
        ControlDto.LastModifiedDateParams params = (ControlDto.LastModifiedDateParams) ctrl.params;
        saveLong(appContext, PREF_DELETE_FILES_TIME, params.modifiedHour);
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
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_ACK);
        sendToServer(nativeProvider.getRenderAck(ad.getSecretKey()));
        AdVersionKeeper.adRendered(ad.getId() + "");
    }

    private void onSubtitleRendered(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_ACK);
        sendToServer(subtitleProvider.getRenderAck(ad));
    }

    void onSplashRendered(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_ACK);
        sendToServer(splashProvider.getRenderAck(ad));
    }

    void onNativeAdClicked(Context context, NativeAd ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_CLICK);
        GoalActionMeter.getInstance(appContext).checkReport(ad.getSecretKey());
        sendToServer(nativeProvider.getClickAck(ad.getSecretKey()));
        if (ad.hasLanding()) {
            nativeProvider.showLanding(context, ad);
        } else {
            nativeProvider.handleLandingClick(context, ad);
        }

    }

    void onSubtitleClicked(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_CLICK);
        GoalActionMeter.getInstance(appContext).checkReport(ad.data.report);
        sendToServer(subtitleProvider.getClickAck(ad));
    }

    void onSplashClicked(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_CLICK);
        GoalActionMeter.getInstance(appContext).checkReport(ad.data.report);
        sendToServer(splashProvider.getClickAck(ad));
    }

    void onNativeAdLandingClicked(NativeAd ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad.getSecretKey(), SimulateClickDto.ON_LANDING_CLICK);
        sendToServer(nativeProvider.getLandingAck(ad.getSecretKey()));
    }

    void onNativeAdLongClick(Context context, NativeAd ad) {
        if (adsAreDismissible)
            nativeProvider.showDismissOptions(context, ad, dismissOptions, instance);
    }

    void onSplashLandingClicked(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_LANDING_CLICK);
        sendToServer(splashProvider.getLandingClickAck(ad));
    }

    void onSubtitleLandingClicked(Ad ad) {
        ClickSimulator.getInstance(appContext).checkSimulateClicks(ad, SimulateClickDto.ON_LANDING_CLICK);
        sendToServer(subtitleProvider.getLandingClickAck(ad));
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
        if (msg != null) {
            ResanaLog.v(TAG, "sendToServer: " + msg);
            BefrestFactory.getInstance(appContext).sendToServer(msg);
        }
    }

    void onReceiveSimulateClicksDone(final Ad ad) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (instance != null) {
                    adReceiver.handleAds(appContext, new Ad[]{ad}, false);
                }
            }
        });
    }

    private class AdReceiver extends BefrestPushReceiver {
        @Override
        public void onPushReceived(Context context, Ad[] ads) {
            handleAds(context, ads, true);
        }

        void handleAds(Context context, Ad[] ads, boolean runOnReceiveSimulateClicks) {
            List<Ad> nonCtrls = new ArrayList<>();
            for (Ad ad : ads) {
                if (ad.isControlMsg()) {
                    handleControlMessage(ad);
                } else {
                    ad.data.ts = "" + System.currentTimeMillis();
                    if (!ad.isInvalid()) {
                        AdVersionKeeper.updateAdVersion(ad);
                        nonCtrls.add(ad);
                    }
                }
            }
            if (nonCtrls.isEmpty())
                return;
            if (runOnReceiveSimulateClicks) {
                for (Ad ad : nonCtrls)
                    ClickSimulator.getInstance(context).runOnReceiveSimulateClicksAndNotifySuccess(ad);
            } else {
                final List<Ad> subtitles = new ArrayList<>();
                final List<Ad> splashes = new ArrayList<>();
                final List<Ad> natives = new ArrayList<>();
                for (Ad ad : nonCtrls) {
                    if (ResanaConfig.gettingSplashAds(context) && ad.getType() == AdDto.AD_TYPE_SPLASH)
                        splashes.add(ad);
//                    else if (ad.getType() == AdDto.AD_TYPE_SUBTITLE)
//                        subtitles.add(ad);
                    else if (ResanaConfig.gettingNativeAds(context) && ad.getType() == AdDto.AD_TYPE_NATIVE)
                        if (!NativeAdProvider.isBlockedZone(ad))  //if received ad is in a blocked zone, we will not add it to list.
                            natives.add(ad);
                }
//                if (subtitles.size() > 0)
//                    subtitleProvider.newAdsReceived(subtitles);
                if (splashes.size() > 0 && ResanaConfig.gettingSplashAds(context))
                    splashProvider.newAdsReceived(splashes);
                if (natives.size() > 0 && ResanaConfig.gettingNativeAds(context))
                    nativeProvider.newAdsReceived(natives);
            }
        }
    }
}