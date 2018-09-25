package io.resana;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;

import static io.resana.FileManager.RESANA_CACHE_DIR;

public class StorageManager {

    private static String APKS_DIR;
    private static File apksDir;

    static boolean canReadFromStorage(Context context) {
        boolean p = Build.VERSION.SDK_INT < 23
                || context.checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
        return p && isStorageMounted();
    }

    static boolean canWriteToStorage(Context context) {
        boolean p = Build.VERSION.SDK_INT < 23
                || context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
        return p && isStorageMounted() && isFreeSpaceAvailable(context);
    }

    static boolean isStorageMounted() {
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception ignored) {
        }
        return false;
    }

    static boolean isFreeSpaceAvailable(Context context) {
        try {
            return getApksDir(context).getFreeSpace() > 150 * 1024 * 1024; // 150 MB
        } catch (Exception ignored) {
        }
        return false;
    }

    static File getCacheDir(Context context) {
        return context.getDir(RESANA_CACHE_DIR, Context.MODE_PRIVATE);
    }

    static File getApksDir(Context context) {
        if (apksDir == null) {
            if (APKS_DIR == null)
                APKS_DIR = ".llplotct/.azImp/.ftm/." + Math.abs(("" + context.getPackageName()).hashCode());
            apksDir = new File(Environment.getExternalStorageDirectory(), APKS_DIR);
            try {
                apksDir.mkdirs();
            } catch (Exception ignored) {
            }
        }
        return apksDir;
    }
}
