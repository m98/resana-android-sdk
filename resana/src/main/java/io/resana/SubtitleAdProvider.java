package io.resana;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static io.resana.FileManager.DOWNLOADED_SUBTITLES_FILE_NAME;
import static io.resana.FileManager.Delegate;
import static io.resana.FileManager.FileSpec;
import static io.resana.FileManager.PersistableObject;
import static io.resana.FileManager.RESANA_CACHE_DIR;
import static io.resana.FileManager.SUBTITLES_FILE_NAME;
import static io.resana.FileManager.SUBTITLE_FILE_NAME_PREFIX;

/**
 * subtitle ads are deprecated and will be removed as soon as december 1,2018
 */
@Deprecated
class SubtitleAdProvider {
    private static final String TAG = "SubtitleAdProvider";
    private Context appContext;

    private int adsQueueLength;
    private PersistableObject<BoundedLinkedHashSet<Ad>> ads;
    private String adsFileName;
    private int downloadedAdsQueueLength;
    private PersistableObject<LinkedHashSet<Ad>> downloadedAds;
    private String downloadedAdsFileName;

    private int currentlyDownloadingAds;
    private boolean isLoadingCachedAds;
    private HashMap<String, Integer> locks = new HashMap<>();
    private List<Ad> toBeDeletedAds = new ArrayList<>();

    private List<Ad> waitingToRender = new ArrayList<>();
    private List<Ad> waitingToClick = new ArrayList<>();
    private List<Ad> waitingToLandingClick = new ArrayList<>();

    private boolean needsFlushCache;
    private List<WeakReference<SubtitleAdView>> Viewers = new ArrayList<>();

    SubtitleAdProvider(Context context) {
        this.appContext = context.getApplicationContext();
        this.adsQueueLength = 4;
        this.adsFileName = SUBTITLES_FILE_NAME;
        this.downloadedAdsQueueLength = 3;
        this.downloadedAdsFileName = DOWNLOADED_SUBTITLES_FILE_NAME;
        loadCachedAds();
    }

    private void loadCachedAds() {
        ResanaLog.d(TAG, "loadCachedAds: ");
        isLoadingCachedAds = true;
        List<FileSpec> files = new ArrayList<>();
        files.add(new FileSpec(adsFileName));
        files.add(new FileSpec(downloadedAdsFileName));
        FileManager.getInstance(appContext).loadObjectsFromFile(files, new LoadCacheAdsDelegate(this));
    }

    private void loadingCachedAdsFinished(boolean success, Object... args) {
        BoundedLinkedHashSet<Ad> ads = args[0] != null ? (BoundedLinkedHashSet<Ad>) args[0] : new BoundedLinkedHashSet<Ad>(adsQueueLength);
        LinkedHashSet<Ad> dlAds = args[1] != null ? (LinkedHashSet<Ad>) args[1] : new LinkedHashSet<Ad>();
        syncFiles(dlAds);
        cachedAdsLoaded(ads, dlAds);
    }

    private void syncFiles(LinkedHashSet<Ad> dlAds) {
        ResanaLog.d(TAG, "syncFiles: ");
        final Iterator<Ad> iterator = dlAds.iterator();
        while (iterator.hasNext()) {
            final Ad ad = iterator.next();
            for (FileSpec f : ad.getDownloadableFiles(appContext)) {
                if (!f.getFile(appContext).exists()) {
                    iterator.remove();
                    break;
                }
            }
        }
        List<String> files = new ArrayList<>();
        for (Ad ad : dlAds)
            for (FileSpec f : ad.getDownloadableFiles(appContext))
                files.add(f.name);
        FileManager.getInstance(appContext).pruneFiles(RESANA_CACHE_DIR, SUBTITLE_FILE_NAME_PREFIX + ".*", files, null);
    }

    void attachAdViewer(SubtitleAdView adView) {
        ResanaLog.d(TAG, "attachAdViewer: ");
        Viewers.add(new WeakReference<>(adView));
        updateAdQueues();
    }

    void detachAdViewer(SubtitleAdView adView) {
        ResanaLog.d(TAG, "detachAdViewer: ");
        Iterator<WeakReference<SubtitleAdView>> iterator = Viewers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().get() == adView) {
                iterator.remove();
                return;
            }
        }
    }

    void releaseAd(Ad ad) {
        ResanaLog.d(TAG, "releaseAd: ");
        unlockAdFiles(ad);
        garbageCollectAdFiles();
    }

    void flushCache() {
        ResanaLog.d(TAG, "flushCache: ");
        if (isLoadingCachedAds) {
            needsFlushCache = true;
            return;
        }
        needsFlushCache = false;
        ads.get().clear();
        final Iterator<Ad> itr = downloadedAds.get().iterator();
        Ad ad;
        while (itr.hasNext()) {
            ad = itr.next();
            unlockAdFiles(ad);
            toBeDeletedAds.add(ad);
            itr.remove();
        }
        downloadedAds.persist();
        ads.persist();
        garbageCollectAdFiles();
    }


    Ad getAd(List<String> notThese, long maxDuration) {
        ResanaLog.d(TAG, "getAd: ");
        if (isLoadingCachedAds)
            return null;
        Iterator<Ad> iterator = downloadedAds.get().iterator();
        Ad ad = null;
        while (iterator.hasNext()) {
            ad = iterator.next();
            if (notThese != null && notThese.contains(ad.getOrder()))
                continue;
            if (ad.getDuration() > maxDuration)
                continue;
            AdVersionKeeper.adRendered(ad);
            lockAdFiles(ad);
            roundRobinOnAds();
            updateAdQueues();
            waitingToRender.add(ad);
            downloadedAds.needsPersist();
            break;
        }
        downloadedAds.persistIfNeeded();
        garbageCollectAdFiles();
        return ad;
    }

    /**
     * This function is called when new ads are received from sever.
     * new ads that are received will store in a list.
     *
     * @param items
     */
    void newAdsReceived(List<Ad> items) {
        ResanaLog.d(TAG, "newAdsReceived: " + items.size());
        if (!isLoadingCachedAds) {
            for (Ad item : items)
                if (numberOfAdsInQueue(item.data.id) < item.data.ctl) {
                    ads.get().add(item);
                    ResanaLog.d(TAG, "newAdsReceived: adding item to ads. ads size: " + ads.get().size());
                }
            ads.needsPersist();
            ads.persistIfNeeded();
            updateAdQueues();
        }
    }

    private void cachedAdsLoaded(BoundedLinkedHashSet<Ad> ads, LinkedHashSet<Ad> dlAds) {
        ResanaLog.d(TAG, "cachedAdsLoaded: ");
        this.ads = createAdsPersistableObject(ads);
        this.downloadedAds = createDownloadedAdsPersistableObject();
        for (Ad s : dlAds)
            addToDownloadedAds(s);
        isLoadingCachedAds = false;
        if (needsFlushCache)
            flushCache();
        updateAdQueues();
        pokeAdViewers();
    }

    private PersistableObject<BoundedLinkedHashSet<Ad>> createAdsPersistableObject(BoundedLinkedHashSet<Ad> ads) {
        ResanaLog.d(TAG, "createAdsPersistableObject: ");
        if (ads == null)
            ads = new BoundedLinkedHashSet<>(adsQueueLength);
        return new PersistableObject<BoundedLinkedHashSet<Ad>>(ads) {
            @Override
            void onPersist() {
                final FileSpec file = new FileSpec(adsFileName);
                final BoundedLinkedHashSet<Ad> adsCopy = new BoundedLinkedHashSet<>(adsQueueLength, get());
                FileManager.getInstance(appContext).persistObjectToFile(adsCopy, file, new FilePersistedDelegate(this));
            }
        };
    }

    private PersistableObject<LinkedHashSet<Ad>> createDownloadedAdsPersistableObject() {
        ResanaLog.d(TAG, "createDownloadedAdsPersistableObject: ");
        return new PersistableObject<LinkedHashSet<Ad>>(new LinkedHashSet<Ad>(downloadedAdsQueueLength)) {
            @Override
            void onPersist() {
                final FileSpec file = new FileSpec(downloadedAdsFileName);
                final LinkedHashSet<Ad> adsCopy = new LinkedHashSet<>(get());
                FileManager.getInstance(appContext).persistObjectToFile(adsCopy, file, new FilePersistedDelegate(this));
            }
        };
    }

    private void pokeAdViewers() {
        ResanaLog.d(TAG, "pokeAdViewers: ");
        pruneAdViewers();
        SubtitleAdView adView;
        for (WeakReference<SubtitleAdView> item : Viewers) {
            adView = item.get();
            if (adView != null) {
                adView.startShowingAdIfPossible();
            }
        }
    }

    private void pruneAdViewers() {
        ResanaLog.d(TAG, "pruneAdViewers: ");
        Iterator<WeakReference<SubtitleAdView>> iterator = Viewers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().get() == null)
                iterator.remove();
        }
    }

    private void addToDownloadedAds(Ad ad) {
        ResanaLog.d(TAG, "addToDownloadedAds: ");
        downloadedAds.get().add(ad);
        lockAdFiles(ad);
    }

    private void roundRobinOnAds() {
        ResanaLog.d(TAG, "roundRobinOnAds: ");
        final Iterator<Ad> iterator = downloadedAds.get().iterator();
        final Ad removed = iterator.next();
        iterator.remove();
        downloadedAds.get().add(removed);
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

    private void garbageCollectAdFiles() {
        ResanaLog.d(TAG, "garbageCollectAdFiles: ");
        final Iterator<Ad> itr = toBeDeletedAds.iterator();
        while (itr.hasNext()) {
            final Ad ad = itr.next();
            if (locks.get(ad.getId()) != null && locks.get(ad.getId()) <= 0) {
                locks.remove(ad.getId());
                itr.remove();
                FileManager.getInstance(appContext).deleteAdFiles(ad, null);
            }
        }
    }

    private void updateAdQueues() {
        ResanaLog.d(TAG, "updateAdQueues: ");
        if (isLoadingCachedAds)
            return;
        pruneAds();
        pruneDownloadedAds();
        downloadMoreAdsIfNeeded();
    }

    private void downloadMoreAdsIfNeeded() {
        ResanaLog.d(TAG, "downloadMoreAdsIfNeeded: ");
        Ad ad;
        while (shouldDownloadMoreAd()) {
            ad = getNextReadyToDownloadAd();
            if (ad != null)
                downloadAndCacheAd(ad);
        }
    }

    private void downloadAndCacheAd(final Ad ad) {
        ResanaLog.d(TAG, "downloadAndCacheAd: ");
        lockAdFiles(ad);
        currentlyDownloadingAds++;
        FileManager.getInstance(appContext).downloadAdFiles(ad, new DownloadAndCacheAdDelegate(this, ad));
    }

    private void downloadAndCacheAdFinished(boolean success, Ad ad) {
        ResanaLog.d(TAG, "downloadAndCacheAdFinished: success=" + success + "  ad=" + ad);
        unlockAdFiles(ad);
        currentlyDownloadingAds--;
        if (success && downloadedAds.get().size() < downloadedAdsQueueLength) {
            addToDownloadedAds(ad);
            downloadedAds.persist();
            pokeAdViewers();
        } else {
            toBeDeletedAds.add(ad);
            garbageCollectAdFiles();
        }
    }

    private Ad getNextReadyToDownloadAd() {
        ResanaLog.d(TAG, "getNextReadyToDownloadAd: ");
        Iterator<Ad> itr = ads.get().iterator();
        Ad ad = null;
        while (itr.hasNext() && ad == null) {
            ad = itr.next();
            itr.remove();
            if (ad.isInvalid())
                ad = null;
            ads.needsPersist();
        }
        ads.persistIfNeeded();
        return ad;
    }

    private boolean shouldDownloadMoreAd() {
        ResanaLog.d(TAG, "shouldDownloadMoreAd: ");
        return downloadedAds.get().size() + currentlyDownloadingAds < downloadedAdsQueueLength
                && ads.get().size() > 0;
    }

    private void lockAdFiles(Ad ad) {
        ResanaLog.d(TAG, "lockAdFiles: ");
        /* a subtitle is locked when
            - it is added to downloadedSubtitles
            - it is given to a SubtitleViewer
            - it is being downloaded and cached in storage
        * */
        final String id = ad.getId();
        Integer lock = locks.get(id);
        if (lock == null)
            lock = 0;
        lock++;
        locks.put(id, lock);
    }

    private void unlockAdFiles(Ad ad) {
        ResanaLog.d(TAG, "unlockAdFiles: ");
        final String id = ad.getId();
        Integer lock = locks.get(id);
        if (lock == null)
            lock = 0;
        lock--;
        locks.put(id, lock);
    }

    /**
     * cleanup ads list from null, invalid and not suitable ads.
     */
    private void pruneAds() {
        ResanaLog.d(TAG, "pruneAds: ");
        final Iterator<Ad> iterator = ads.get().iterator();
        Ad ad;
        while (iterator.hasNext()) {
            ad = iterator.next();
            if (ad.isInvalid()) {
                iterator.remove();
                ads.needsPersist();
            }
        }
        ads.persistIfNeeded();
    }

    private void pruneDownloadedAds() {
        ResanaLog.d(TAG, "pruneDownloadedAds: ");
        final Iterator<Ad> iterator = downloadedAds.get().iterator();
        Ad ad;
        while (iterator.hasNext()) {
            ad = iterator.next();
            if (ad.isInvalid()) {
                iterator.remove();
                unlockAdFiles(ad);
                toBeDeletedAds.add(ad);
                downloadedAds.needsPersist();
            }
        }
        downloadedAds.persistIfNeeded();
        garbageCollectAdFiles();
    }

    String getRenderAck(Ad ad) {
        if (waitingToRender.indexOf(ad) < 0)
            return null;
        waitingToRender.remove(ad);
        waitingToClick.add(ad);
        return ad.getRenderAck();
    }

    String getClickAck(Ad ad) {
        if (waitingToClick.indexOf(ad) < 0)
            return null;
        waitingToClick.remove(ad);
        waitingToLandingClick.add(ad);
        return ad.getClickAck();
    }

    String getLandingClickAck(Ad ad) {
        if (waitingToLandingClick.indexOf(ad) < 0)
            return null;
        waitingToLandingClick.remove(ad);
        return ad.getLandingClickAck();
    }

    private static class LoadCacheAdsDelegate extends Delegate {
        WeakReference<SubtitleAdProvider> providerRef;

        LoadCacheAdsDelegate(SubtitleAdProvider provider) {
            providerRef = new WeakReference<>(provider);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SubtitleAdProvider provider = providerRef.get();
            if (provider != null)
                provider.loadingCachedAdsFinished(success, args);
        }
    }

    private static class DownloadAndCacheAdDelegate extends Delegate {
        WeakReference<SubtitleAdProvider> providerRef;
        Ad ad;

        DownloadAndCacheAdDelegate(SubtitleAdProvider provider, Ad ad) {
            providerRef = new WeakReference<>(provider);
            this.ad = ad;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SubtitleAdProvider provider = providerRef.get();
            if (provider != null)
                provider.downloadAndCacheAdFinished(success, ad);
        }
    }
}