package io.resana;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.resana.FileManager.Delegate;
import static io.resana.FileManager.PersistableObject;

class NativeAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "NativeAdProvider";
    private static final String CLICK_ACK_PREFS = "RESANA_CLICK_ACK_PREFS" + 2;

    private static NativeAdProvider instance;
    private Context appContext;
    private int adsQueueLength;
    private PersistableObject<Set<Ad>> ads;
    private Map<String, List<Ad>> adsMap;
    private List<String> downloadedAds;
    private static String[] blockedZones;

    static NativeAdProvider getInstance(Context context) {
        NativeAdProvider localInstance = instance;
        synchronized (NativeAdProvider.class) {
            localInstance = instance;
            if (localInstance == null) {
                localInstance = instance = new NativeAdProvider(context);
            }
        }
        return localInstance;
    }

    private NativeAdProvider(Context context) {
        this.appContext = context;
        this.adsQueueLength = 4;
        adsMap = new HashMap<>();
        downloadedAds = Collections.synchronizedList(new ArrayList<String>());
        loadBlockedZones();
        NetworkManager.getInstance().getNativeAds(new AdsReceivedDelegate(appContext));
    }

    static boolean isBlockedZone(String zone) {
        if (blockedZones == null || zone == null || zone.equals(""))
            return false;
        return Arrays.asList(blockedZones).contains(zone);
    }

    private void persistBlockedZones() {
        ResanaLog.d(TAG, "persistBlockedZones: ");
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

    private void newAdsReceived(List<Ad> items) {
        pruneAds(items);
        newAdsReceived(items, "");
    }

    private void newAdsReceived(List<Ad> items, String zone) {
        pruneAds(items);
        ResanaLog.e(TAG, "newAdsReceived: ads size=" + items.size() + (zone.equals("") ? "" : (" zone=" + zone)));
        if (!zone.equals("")) {
            List<Ad> list = adsMap.get(zone);
            if (list == null)
                list = new ArrayList<>();
            if (list.size() >= adsQueueLength)
                return;
            for (Ad item : items) {
                if (item.data.hot)
                    list.add(0, item);
                else list.add(item);
            }
        } else {
            for (Ad item : items) {
                String[] zones = item.data.zones;
                for (String adZone : zones) {
                    List<Ad> list = adsMap.get(adZone);
                    if (list == null)
                        list = new ArrayList<>();
                    if (list.size() >= adsQueueLength)
                        break;
                    if (item.data.hot)
                        list.add(0, item);
                    else list.add(item);
                    adsMap.put(adZone, list);
                }
            }
        }

        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            Log.e(TAG, "zone: " + entry.getKey());
            List<Ad> ad = entry.getValue();
            for (Ad a :
                    ad) {
                Log.e(TAG, "ads: " + a.getId());
            }
        }
        downloadFirstAdOfList();
    }

    /**
     * prune all ads
     */
    private void pruneAds() {
        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            pruneAds(entry.getValue());
        }
    }

    /**
     * prune ads of a list (zone)
     *
     * @param ads
     */
    private void pruneAds(List<Ad> ads) {
        List<Ad> toRemove = new ArrayList<>();
        for (Ad ad : ads) {
            if (ad.isInvalid(appContext))
                toRemove.add(ad);
        }
        for (Ad ad : toRemove) {
            ads.remove(ad);
        }
    }

    private void downloadAdFiles(final Ad ad, Delegate delegate) {
        VisualsManager.saveVisualsIndex(appContext, ad);
        FileManager.getInstance(appContext).downloadAdFiles(ad, delegate);
    }

    private void downloadAdFiles(String zone) {
        ResanaLog.d(TAG, "downloadAdFiles: Downloading ad of list " + zone);
        List<Ad> ads = adsMap.get(zone);
        if (ads == null || ads.size() == 0)
            return;
        Ad shouldDownloadAd = ads.get(0);
        if (isDownloaded(shouldDownloadAd)) {
            ResanaLog.d(TAG, "downloadAdFiles: ad " + shouldDownloadAd.getId() + " is downloaded before");
            return;
        }
        DownloadAdFilesDelegate delegate = new DownloadAdFilesDelegate(appContext, zone, shouldDownloadAd);
        downloadAdFiles(shouldDownloadAd, delegate);
    }

    private void downloadFirstAdOfList() {
        ResanaLog.d(TAG, "downloadFirstAdOfList: Downloading first ad of every list");
        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            downloadAdFiles(entry.getKey());
        }
    }

    private void adDownloaded(Ad ad) {
        ResanaLog.d(TAG, "adDownloaded: ad " + ad.getId() + " downloaded");
        downloadedAds.add(ad.getId());
    }

    private boolean isDownloaded(Ad ad) {
        return downloadedAds.contains(ad.getId());
    }

    private void roundRobinOnAds() {
        ResanaLog.d(TAG, "roundRobinOnAds: ");
        final Iterator<Ad> itr = ads.get().iterator();
        Ad remove = itr.next();
        itr.remove();
        ads.get().add(remove);
    }

    private void loadBlockedZones() {
        ResanaLog.d(TAG, "loadBlockedZones: ");
        String s = ResanaPreferences.getString(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES, null);
        if (s != null && s.length() > 0) {
            String[] zones = s.split(";");
            blockedZones = new String[zones.length];
            for (int i = 0; i < zones.length; i++) {
                blockedZones[i] = zones[i];
                ResanaLog.d(TAG, "blockedZone: " + blockedZones[i]);
            }
        } else {
            blockedZones = null;
            ResanaLog.d(TAG, "loadBlockedZones: there is no block zone");
        }

    }

    private Ad internalGetAd(boolean hasTitle, String zone) {
        if (adsMap == null) {
            return null;
        }
        List<Ad> adList = adsMap.get(zone);
        if (adList == null) {
            ResanaLog.e(TAG, "getAd: no such " + zone + " zone");
            return null;
        }
        if (adList.size() <= 2)
            NetworkManager.getInstance().getNativeAds(new AdsReceivedDelegate(appContext, zone));
        if (adList.size() == 0)
            return null;
        Ad ad = adList.get(0);
        if (isDownloaded(ad)) { //todo double check here. round robin on ad list
            adList.remove(ad);
            if (ad.data.hot)
                adList.add(0, ad);
            else adList.add(ad);
            return ad;
        }
        return null;
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

    NativeAd getAd(boolean hasTitle, String zone) {
        ResanaLog.d(TAG, "getAd: ");
        if (ResanaInternal.instance.isInDismissRestTime()) {
            ResanaLog.d(TAG, "getAd: Native dismissRestTime");
            return null;
        }

        if (isBlockedZone(zone)) {
            ResanaLog.e(TAG, "getAd: zone " + zone + " is blocked");
            return null;
        }
        final Ad ad = internalGetAd(hasTitle, zone);
        if (ad != null) {
            adsMap.get(zone).remove(0);
            downloadAdFiles(zone);
            return new NativeAd(appContext, ad, AdDatabase.getInstance(appContext).generateSecretKey(ad));
        }
        return null;
    }

    void onNativeAdRendered(NativeAd ad) {
        NetworkManager.getInstance().sendReports(Reports.view, ad.getId() + "");
        AdVersionKeeper.adRendered(ad.getId() + "");
        pruneAds();
    }

    void onNativeAdClicked(Context context, NativeAd ad, AdDelegate delegate) {
        NetworkManager.getInstance().sendReports(Reports.click, ad.getId() + "");
        if (ad.hasLanding()) {
            showLanding(context, ad, delegate);
        } else {
            handleLandingClick(context, ad, delegate);
        }
    }

    private void onNativeAdLandingClicked(NativeAd ad) {
        NetworkManager.getInstance().sendReports(Reports.landingClick, ad.getId() + "");
    }

    private void showLanding(final Context context, final NativeAd ad, final AdDelegate adDelegate) {
        final NativeLandingView nativeLandingView = new NativeLandingView(context, ad);
        nativeLandingView.setDelegate(new LandingView.Delegate() {
            @Override
            public void closeLanding() {
                nativeLandingView.dismiss();
            }

            @Override
            public void landingActionClicked() {
                handleLandingClick(context, ad, adDelegate);
                onNativeAdLandingClicked(ad);
                nativeLandingView.dismiss();
            }
        });
        nativeLandingView.show();
    }

    private void handleLandingClick(final Context context, final NativeAd ad, AdDelegate adDelegate) {
        if (ResanaInternal.instance == null)
            return;
        if (ApkManager.getInstance(context).isApkDownloading(context, ad)) {
            if (adDelegate == null)
                Toast.makeText(appContext, "در حال آماده سازی", Toast.LENGTH_SHORT).show();
            else adDelegate.onPreparingProgram();
            return;
        }
        if (ad.hasApk()) {
            ApkManager.getInstance(context).downloadAndInstallApk(ad, adDelegate);
        } else if (ad.hasIntent()) {
            Intent intent = ad.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(intent, "انتخاب کنید"));
            else
                ResanaLog.e(TAG, "handleLandingClick: unable to resolve intent");
        } else if (ad.hasLink()) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(Uri.parse(ad.getLink()));
            if (i.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(i, "انتخاب کنید"));
            else
                ResanaLog.e(TAG, "handleLandingClick: unable to resolve link intent");
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

    private static class AdsReceivedDelegate extends Delegate {
        Context context;
        String zone = "";

        AdsReceivedDelegate(Context context) {
            this(context, "");
        }

        AdsReceivedDelegate(Context context, String zone) {
            this.context = context;
            this.zone = zone;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            if (success)
                NativeAdProvider.getInstance(context).newAdsReceived((List<Ad>) args[0], zone);
        }
    }

    private static class DownloadAdFilesDelegate extends Delegate {
        Context context;
        String zone;
        Ad downloadedAd;

        DownloadAdFilesDelegate(Context context, String zone, Ad ad) {
            this.context = context;
            this.zone = zone;
            this.downloadedAd = ad;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            ResanaLog.d(TAG, "Download ad files of list " + zone + ". result=" + success);
            if (!success) {
                List<Ad> ad = NativeAdProvider.getInstance(context).adsMap.get(zone);
                if (ad == null || ad.size() == 0)
                    return;
                ad.remove(downloadedAd);
                NativeAdProvider.getInstance(context).downloadAdFiles(zone);
            } else {
                NativeAdProvider.getInstance(context).adDownloaded(downloadedAd);
            }
        }
    }

    class Reports {
        static final String view = "view";
        static final String click = "click1";
        static final String landingClick = "click2";
        static final String install = "install";
    }
}
