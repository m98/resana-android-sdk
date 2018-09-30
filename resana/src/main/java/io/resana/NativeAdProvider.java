package io.resana;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.resana.FileManager.Delegate;
import static io.resana.FileManager.FileSpec;
import static io.resana.FileManager.NATIVE_ADS_FILE_NAME;
import static io.resana.FileManager.PersistableObject;

class NativeAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "NativeAdProvider";
    private static final String CLICK_ACK_PREFS = "RESANA_CLICK_ACK_PREFS" + 2;

    private Context appContext;
    private String adsFileName;
    private int adsQueueLength;
    private PersistableObject<Set<Ad>> ads;
    private Set<Ad> noZoneAds;
    private Set<Ad> zoneAds;
    private Map<String, Acks> waitingToBeRenderedByClient = new HashMap<>();
    private Map<String, Acks> waitingForLandingClick = new HashMap<>();
    private ExpiringSharedPreferences clickAckPrefs;
    private static String[] blockedZones;
    private int NO_ZONE_ADS = 0, ZONE_ADS = 1;
    boolean isLoadingCacheAds = true;

    private boolean needsFlushCache;


    NativeAdProvider(Context context) {
        this.appContext = context;
        this.adsFileName = NATIVE_ADS_FILE_NAME;
        this.adsQueueLength = 7;
        this.zoneAds = new BoundedLinkedHashSet<>(adsQueueLength);
        this.noZoneAds = new BoundedLinkedHashSet<>(adsQueueLength);
        loadCachedAds();
        loadBlockedZones();
        clickAckPrefs = new ExpiringSharedPreferences(appContext, CLICK_ACK_PREFS, 24 * 60 * 60 * 1000);
    }

    private void cachedAdsLoaded(Set<Ad> ads) {
        isLoadingCacheAds = false;
        this.ads = createAdsPersistableObject(ads);
        if (needsFlushCache)
            flushCache();
        ResanaLog.d(TAG, "cachedAdsLoaded: size " + this.ads.get().size());
        separateAds();
    }

    /**
     * This function will separate all ads and create no zone and zone ads list
     */
    private void separateAds() {
        ResanaLog.d(TAG, "separateAds: separating ads");
        final Iterator<Ad> itr = ads.get().iterator();
        while (itr.hasNext()) {
            Ad ad = itr.next();
            if (ad.data.zones == null || ad.data.zones.length == 0) //this ad does not have zone
                noZoneAds.add(ad);
            else
                zoneAds.add(ad);
        }
    }

    private void persistBlockedZones() {
        if (blockedZones == null || blockedZones.length == 0)
            ResanaPreferences.remove(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES);
        else {
            String s = "";
            for (int i = 0; i < blockedZones.length; i++) {
                s += blockedZones[i];
                s += ";";
            }
            ResanaPreferences.saveString(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES, s);
        }
    }

    private void loadBlockedZones() {
        ResanaLog.d(TAG, "loadBlockedZones: loading blocked zones");
        String s = ResanaPreferences.getString(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES, null);
        if (s != null && s.length() > 0) {
            String[] zones = s.split(";");
            blockedZones = new String[zones.length - 1];
            for (int i = 0; i < zones.length - 1; i++) {
                blockedZones[i] = zones[i];
            }
        } else
            blockedZones = null;

    }

    void handleBlockedZones(ControlDto ctrl) {
        ControlDto.BlockedZonesParams params = (ControlDto.BlockedZonesParams) ctrl.params;
        if (params.zones == null || params.zones.length == 0) {
            blockedZones = null;
        } else {
            blockedZones = new String[params.zones.length];
            for (int i = 0; i < params.zones.length; i++) {
                blockedZones[i] = params.zones[i];
            }
            persistBlockedZones();
        }
    }

    private void loadCachedAds() {
        isLoadingCacheAds = true;
        FileManager.getInstance(appContext).loadObjectFromFile(new FileManager.FileSpec(adsFileName), new NativeAdProvider.LoadCachedAdsDelegate(this));
    }

    private PersistableObject<Set<Ad>> createAdsPersistableObject(Set<Ad> loaded) {
        final Set<Ad> ads = Collections.synchronizedSet(new BoundedLinkedHashSet<Ad>(adsQueueLength));
        if (loaded != null)
            ads.addAll(loaded);

        return new PersistableObject<Set<Ad>>(ads) {
            @Override
            void onPersist() {
                final FileSpec file = new FileSpec(adsFileName);
                Set<Ad> adsCopy = new BoundedLinkedHashSet<>(adsQueueLength);
                adsCopy.addAll(get());
                FileManager.getInstance(appContext).persistObjectToFile(adsCopy, file, new FilePersistedDelegate(this));
            }
        };
    }

    void newAdsReceived(List<Ad> items) {
        ResanaLog.d(TAG, "newAdsReceived: new ads received");
        if (!isLoadingCacheAds) {
            for (Ad item : items) {
                if (!ApkManager.getInstance(appContext).isApkInstalled(item)) {
                    downloadAdFiles(item);
                    if (numberOfAdsInQueue(item.data.id) < item.data.ctl) {
                        ads.get().add(item);
                        ResanaLog.d(TAG, "newAdsReceived: adding item to ads. ads size: " + ads.get().size());
                    }
                }
            }
            ads.needsPersist();
            ads.persistIfNeeded();
            separateAds();
        }
    }

    void flushCache() {
        if (noZoneAds == null || zoneAds == null) {
            needsFlushCache = true;
            return;
        }
        ads.get().clear();
        noZoneAds.clear();
        zoneAds.clear();
        ads.persist();
        needsFlushCache = false;
    }

    private void downloadAdFiles(final Ad ad) {
        VisualsManager.saveVisualsIndex(appContext, ad);
        FileManager.getInstance(appContext).downloadAdFiles(ad, new Delegate() {
            @Override
            void onFinish(boolean success, Object... args) {
                //todo should find a better way for downloading ads assets in background
                ResanaLog.d(TAG, "onFinish: downloading native ads file finishes. success " + success);
                ResanaPreferences.saveBoolean(appContext, ad.getId() + "ddd", success);
                ad.data.ts = "" + System.currentTimeMillis();
            }
        });
    }

    private void pruneAds() {
        if (ads == null)
            return;
        final Iterator<Ad> zoneItr = zoneAds.iterator();
        Ad zoneAd;
        while (zoneItr.hasNext()) {
            zoneAd = zoneItr.next();
            if (zoneAd.isInvalid()
                    || !ResanaPreferences.getBoolean(appContext, zoneAd.getId() + "ddd", false)) {
                zoneItr.remove();
                ads.get().remove(zoneAd);
                ads.needsPersist();
            }
        }
        final Iterator<Ad> noZoneItr = noZoneAds.iterator();
        Ad noZoneAd;
        while (noZoneItr.hasNext()) {
            noZoneAd = noZoneItr.next();
            if (noZoneAd.isInvalid()
                    || !ResanaPreferences.getBoolean(appContext, noZoneAd.getId() + "ddd", false)) {
                noZoneItr.remove();
                ads.get().remove(noZoneAd);
                ads.needsPersist();
            }
        }
        ads.persistIfNeeded();
    }

    private void roundRobinOnAds(int type) {
        if (type == NO_ZONE_ADS) {
            final Iterator<Ad> noZoneAdItr = noZoneAds.iterator();
            Ad remove = noZoneAdItr.next();
            noZoneAdItr.remove();
            noZoneAds.add(remove);
        } else {
            final Iterator<Ad> zoneAdItr = zoneAds.iterator();
            Ad remove = zoneAdItr.next();
            zoneAdItr.remove();
            zoneAds.add(remove);
        }
        ads.get().clear();
        ads.get().addAll(noZoneAds);
        ads.get().addAll(zoneAds);
    }

    private int numberOfAdsInQueue(long adId) {
        int res = 0;
        Iterator<Ad> adsItr = ads.get().iterator();
        while (adsItr.hasNext()) {
            Ad ad = adsItr.next();
            if (ad.data.id == adId)
                res++;
        }
        return res;
    }

    String getRenderAck(String secretKey) {
        final Acks acks = waitingToBeRenderedByClient.get(secretKey);
        if (acks != null) {
            waitingToBeRenderedByClient.remove(secretKey);
            acks.saveToPrefs(clickAckPrefs, secretKey);
            return acks.render;
        }
        return null;
    }

    String getClickAck(String secretKey) {
        final Acks acks = new Acks().fromPrefs(clickAckPrefs, secretKey);
        if (acks.click != null) {
            acks.removeFromPref(clickAckPrefs, secretKey);
            waitingForLandingClick.put(secretKey, acks);
        }
        return acks.click;
    }

    String getLandingAck(String secretKey) {
        final Acks acks = waitingForLandingClick.get(secretKey);
        waitingForLandingClick.remove(secretKey);
        if (acks != null) {
            return acks.landing;
        }
        return null;
    }

    NativeAd getAd(boolean hasTitle, String zone) {
        if (ResanaInternal.instance.isInDismissRestTime()) {
            ResanaLog.d(TAG, "get: Native dismissRestTime");
            return null;
        }
        if (isLoadingCacheAds)
            return null;
        final Ad ad = internalGetAd(hasTitle, zone);
        if (ad != null) {
            final NativeAd nativeAd = new NativeAd(appContext, ad, AdDatabase.getInstance(appContext).generateSecretKey(ad));
            final Acks acks = new Acks(ad);
            waitingToBeRenderedByClient.put(nativeAd.getSecretKey(), acks);
            GoalActionMeter.getInstance(appContext).persistReport(nativeAd.getSecretKey(), ad.data.report);
            ClickSimulator.getInstance(appContext).persistSimulateClicks(nativeAd.getSecretKey(), ad.data.simulateClicks);
            return nativeAd;
        }
        return null;
    }

    private Ad internalGetAd(boolean hasTitle, String zone) {
        pruneAds();
        int typeOfAd = -1;
        if (ads == null || ads.get().isEmpty()) {
            if (ads == null)
                ResanaLog.e(TAG, "get: ads is null");
            if (ads.get().isEmpty())
                ResanaLog.e(TAG, "get: ads is empty");
            ResanaLog.e(TAG, "get: no native ad available.");
            return null;
        }
        final boolean cooldown = !CoolDownHelper.shouldShowNativeAd(appContext);
        Ad result = null;
        if (!zone.equals("")) {
            final Ad hotZoneAd = nextReadyToRenderZoneAd(true, zone, hasTitle);
            if (hotZoneAd != null) {
                result = hotZoneAd;
                typeOfAd = ZONE_ADS;
            } else { // there was no zone hot ad. so checking no zone hot ads.
                Ad hotNoZonedAd = nextReadyToRenderNoZoneAd(true, hasTitle);
                if (hotNoZonedAd != null) {
                    result = hotNoZonedAd;
                    typeOfAd = NO_ZONE_ADS;
                } else { //there were not any no zone hot ad. so checking not hot zone ads.
                    Ad noHotZoneAd = nextReadyToRenderZoneAd(false, zone, hasTitle);
                    if (noHotZoneAd != null) {
                        result = noHotZoneAd;
                        typeOfAd = ZONE_ADS;
                    } else { //there were not any zone not hot ad. so checking not hot no zone ads.
                        Ad noHotNoZoneAd = nextReadyToRenderNoZoneAd(false, hasTitle);
                        if (noHotNoZoneAd != null) {
                            result = noHotNoZoneAd;
                            typeOfAd = NO_ZONE_ADS;
                        }
                    }
                }
            }
        }
        if (zone.equals("")) {
            Ad hotNoZoneAd = nextReadyToRenderNoZoneAd(true, hasTitle);
            if (hotNoZoneAd != null) {
                result = hotNoZoneAd;
                typeOfAd = ZONE_ADS;
            } else {
                Ad notHotNoZoneAd = nextReadyToRenderNoZoneAd(false, hasTitle);
                if (notHotNoZoneAd != null) {
                    result = notHotNoZoneAd;
                    typeOfAd = NO_ZONE_ADS;
                }
            }
        }

        if (result != null) {
            if (!result.data.hot) {
                roundRobinOnAds(typeOfAd);
                ads.persist();
            }
            if (!cooldown)
                return result;
            else return null;
        }
        return null;
    }


    private Ad nextReadyToRenderNoZoneAd(boolean hotOnly, boolean hasTitle) {//todo
        final Iterator<Ad> iterator = noZoneAds.iterator();
        if (!hotOnly) {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle)
                    return ad;
                else if (hasTitle(ad))
                    return ad;
            }
        } else {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle) {
                    if (ad.data.hot)
                        return ad;
                } else if (ad.data.hot && hasTitle(ad))
                    return ad;
            }
        }
        return null;
    }

    private Ad nextReadyToRenderZoneAd(boolean hotOnly, String zone, boolean hasTitle) {
        final Iterator<Ad> iterator = zoneAds.iterator();
        if (!hotOnly) {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle) {
                    if (validZone(ad.data.zones, zone))
                        return ad;
                } else if (validZone(ad.data.zones, zone) && hasTitle(ad))
                    return ad;
            }
        } else {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle) {
                    if (ad.data.hot && validZone(ad.data.zones, zone))
                        return ad;
                } else if (ad.data.hot && validZone(ad.data.zones, zone) && hasTitle(ad))
                    return ad;
            }
        }
        return null;
    }

    /**
     * will check that should show an ad with a specific zone
     *
     * @param zones zones of an ad
     * @param zone  a zone we want to get ad for
     * @return
     */
    private boolean validZone(String[] zones, String zone) {
        if (zone.equals(""))
            return true;
        if (zones == null || zones.length == 0)
            return true;
        if (blockedZones != null)
            return Arrays.asList(zones).contains(zone) && !Arrays.asList(blockedZones).contains(zone);
        else
            return Arrays.asList(zones).contains(zone);
    }

    /**
     * will check whether an ad has a title or not
     *
     * @param ad
     * @return
     */
    private boolean hasTitle(Ad ad) {
        return ((NativeDto) ad.data).texts != null && ((NativeDto) ad.data).texts.titleText != null;
    }

    /**
     * will check an ad is in a blocked zone or not
     *
     * @param ad
     * @return
     */
    static boolean isBlockedZone(Ad ad) {
        String[] adZones = ((NativeDto) ad.data).zones;
        if (adZones == null || adZones.length == 0
                || blockedZones == null || blockedZones.length == 0)
            return false;
        for (int i = 0; i < adZones.length; i++) {
            if (Arrays.asList(blockedZones).contains(adZones[i]))
                return true;
        }
        return false;
    }

    void showLanding(final Context context, final NativeAd ad) {
        final NativeLandingView nativeLandingView = new NativeLandingView(context, ad);
        nativeLandingView.setDelegate(new LandingView.Delegate() {
            @Override
            public void closeLanding() {
                nativeLandingView.dismiss();
            }

            @Override
            public void landingActionClicked() {
                handleLandingClick(context, ad);
                nativeLandingView.dismiss();
            }
        });
        nativeLandingView.show();
    }

    void handleLandingClick(final Context context, final NativeAd ad) {
        ResanaInternal.getInstance(context, null).onNativeAdLandingClicked(ad);
        if (ad.hasApk()) {
            Toast.makeText(context, "در حال آماده سازی", Toast.LENGTH_SHORT).show();
            FileManager.getInstance(context).downloadFile(new FileSpec(ad.getApkUrl(), FileSpec.DIR_TYPE_APKS, ad.getApkFileName()), false, new Delegate() {
                @Override
                void onFinish(boolean success, Object... args) {
                    if (!success) {
                        Toast.makeText(context, "مشکلی در آماده سازی برنامه به وجود آمده است", Toast.LENGTH_SHORT).show();
                    }
                    else if (success) {
                        File apk = new FileManager.FileSpec(FileSpec.DIR_TYPE_APKS, ad.getApkFileName()).getFile(context);
                        Log.e(TAG, "onFinish: file: " + apk.getAbsolutePath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri n = ResanaFileProvider.getUriForFile(context, context.getPackageName() + ".provider", apk);
                        if (Build.VERSION.SDK_INT >= 24)
                            i.setDataAndType(n, "application/vnd.android.package-archive");
                        else
                            i.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
                        context.startActivity(i);
                    }
                }
            });
        } else if (ad.hasIntent()) {
            Intent intent = ad.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else if (ad.hasLink()) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(Uri.parse(ad.getLink()));
            context.startActivity(i);
        }
    }

    void showDismissOptions(Context context, final NativeAd ad, List<DismissOption> dismissOptions, final ResanaInternal resanaInstance) {
        DismissOptionsView.Delegate delegate = new DismissOptionsView.Delegate() {
            @Override
            public void itemSelected(String key, String reason) {
                resanaInstance.onAdDismissed(ad.getSecretKey(), new DismissOption(key, reason));
            }
        };
        DismissOptionsView dismissOptionsView = new DismissOptionsView(context, dismissOptions, delegate);
        dismissOptionsView.setDismissOptions(dismissOptions);
        dismissOptionsView.show();
    }

    private static class Acks {

        String order;

        String render;
        String click;
        String landing;

        Acks() {

        }

        Acks(Ad ad) {
            this.order = ad.getOrder();
            this.render = ad.getRenderAck();
            this.click = ad.getClickAck();
            this.landing = ad.getLandingClickAck();
        }

        void saveToPrefs(SharedPreferences prefs, String key) {
            final SharedPreferences.Editor editor = prefs.edit();
            if (order != null)
                editor.putString(key + "_order", order);
            if (render != null)
                editor.putString(key + "_render", render);
            if (click != null)
                editor.putString(key + "_click", click);
            if (landing != null)
                editor.putString(key + "_land", landing);
            editor.apply();
        }

        Acks fromPrefs(SharedPreferences prefs, String key) {
            order = prefs.getString(key + "_order", null);
            render = prefs.getString(key + "_render", null);
            click = prefs.getString(key + "_click", null);
            landing = prefs.getString(key + "_land", null);
            return this;
        }

        void removeFromPref(SharedPreferences prefs, String key) {
            prefs.edit()
                    .remove(key + "_order")
                    .remove(key + "_render")
                    .remove(key + "_click")
                    .remove(key + "_land")
                    .apply();
        }
    }

    private static class LoadCachedAdsDelegate extends Delegate {
        WeakReference<NativeAdProvider> providerRef;

        LoadCachedAdsDelegate(NativeAdProvider provider) {
            this.providerRef = new WeakReference<>(provider);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final NativeAdProvider provider = providerRef.get();
            if (provider != null)
                provider.cachedAdsLoaded((Set<Ad>) args[0]);
        }
    }
}
