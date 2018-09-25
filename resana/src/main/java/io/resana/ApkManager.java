package io.resana;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.resana.StorageManager.canReadFromStorage;
import static io.resana.StorageManager.canWriteToStorage;
import static io.resana.StorageManager.getApksDir;

class ApkManager {
    private static final String PREFS = "RESANA_APKS_76432369";

    private static ApkManager instance;
    private Context appContext;
    private List<String> installedPackages;
    private long lastPackagesUpdateTime;

    static ApkManager getInstance(Context context) {
        ApkManager localInstance = instance;
        if (localInstance == null) {
            synchronized (ApkManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new ApkManager(context);
                }
            }
        }
        return localInstance;
    }

    private ApkManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    boolean isApkInstalled(String pkg) {
        return getInstalledPackages().contains(pkg);
    }

    List<String> getInstalledPackages() {
        if (installedPackages != null && System.currentTimeMillis() - lastPackagesUpdateTime < 15000)
            return installedPackages;
        installedPackages = getInstalledPackagesFromSystem();
        lastPackagesUpdateTime = System.currentTimeMillis();
        return installedPackages;
    }

    List<String> getInstalledPackagesFromSystem() {
        List<String> pkgs = new ArrayList<>();
        final List<ApplicationInfo> apps = appContext.getPackageManager().getInstalledApplications(0);
        if (apps != null)
            for (ApplicationInfo app : apps)
                if (app != null && !TextUtils.isEmpty(app.packageName) && (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                    pkgs.add(app.packageName);
        return pkgs;
    }

}