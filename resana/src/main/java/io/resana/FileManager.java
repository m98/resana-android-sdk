package io.resana;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.AsyncTask;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.resana.ResanaInternal.DB_VERSION;
import static io.resana.ResanaPreferences.PREF_DELETE_FILES_TIME;
import static io.resana.ResanaPreferences.PREF_SHOULD_CLEANUP_OLD_FILES;
import static io.resana.ResanaPreferences.getBoolean;
import static io.resana.ResanaPreferences.getLong;
import static io.resana.ResanaPreferences.remove;
import static io.resana.ResanaPreferences.saveBoolean;

class FileManager {
    private static final String TAG = ResanaLog.TAG_PREF + "FileManager";
    private static final String RESANA_CACHE_DIR_PREF = "resanaTemp";
    static final String RESANA_CACHE_DIR = RESANA_CACHE_DIR_PREF + ResanaInternal.DB_VERSION;
    static final String NATIVE_ADS_FILE_NAME = "natives-with-zones";

    static final String SPLASHES_FILE_NAME = "splashes";
    static final String DOWNLOADED_SPLASHES_FILE_NAME = "dlSplashes";
    static final String SPLASH_FILE_NAME_PREFIX = "splsh";

    static final String NATIVE_FILE_NAME_PREFIX = "native";

    static final String SUBTITLES_FILE_NAME = "subtitles";
    static final String DOWNLOADED_SUBTITLES_FILE_NAME = "dlSubtitles";
    static final String SUBTITLE_FILE_NAME_PREFIX = "sbt";

    static final String LANDING_IMAGE_PREF = "land";
    static final String CUSTOM_LABEL_PREF = "label";

    static abstract class Delegate {
        abstract void onFinish(boolean success, Object... args);
    }

    private static FileManager instance;

    private Context appContext;

    private Executor downloadExecutor;
    private Executor commonExecutor;
    private Executor apkDlExecutor;

    static FileManager getInstance(Context context) {
        FileManager localInstance = instance;
        if (localInstance == null) {
            synchronized (FileManager.class) {
                localInstance = instance;
                if (localInstance == null)
                    localInstance = instance = new FileManager(context);
            }
        }
        return localInstance;
    }

    private FileManager(Context context) {
        this.appContext = context.getApplicationContext();
        downloadExecutor = new ThreadPoolExecutor(0, 1, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new ResanaThreadFactory("Resana_FM_DlPool"));
        commonExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ResanaThreadFactory("Resana_FM_CommonPool"));
        apkDlExecutor = new ThreadPoolExecutor(0, 1, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new ResanaThreadFactory("Resana_FM_ApkDlPool"));
    }

    void cleanupOldFilesIfNeeded() {
        ResanaLog.d(TAG, "cleanupOldFilesIfNeeded: ");
        if (getBoolean(appContext, PREF_SHOULD_CLEANUP_OLD_FILES + DB_VERSION, true)) {
            new CleanupOldFiles(appContext).execute();
            saveBoolean(appContext, PREF_SHOULD_CLEANUP_OLD_FILES + DB_VERSION, false);
        }
    }

    void deleteOldFiles() {
        List<FileSpec> toDelete = new ArrayList<>();
        File resanaCacheDir = StorageManager.getCacheDir(appContext);
        File resanaApkDir = StorageManager.getApksDir(appContext);
        String[] fileNames = resanaCacheDir.list();
        if (fileNames == null)
            return;
        long deleteTime = getTimeToDeleteOldFiles();
        for (int i = 0; i < fileNames.length; i++) {
            File f = new File(resanaCacheDir, fileNames[i]);
            if ((System.currentTimeMillis() - f.lastModified()) >= deleteTime)
                toDelete.add(new FileSpec(fileNames[i]));
        }
        fileNames = resanaApkDir.list();
        if (fileNames == null)
            return;
        for (int i = 0; i < fileNames.length; i++) {
            File f = new File(resanaApkDir, fileNames[i]);
            if ((System.currentTimeMillis() - f.lastModified()) >= deleteTime)
                toDelete.add(new FileSpec(FileSpec.DIR_TYPE_APKS, fileNames[i]));
        }
        deleteFiles(toDelete, null);
    }

    long getTimeToDeleteOldFiles() {
        return getLong(appContext, PREF_DELETE_FILES_TIME, 259200000);//3 days
    }

    void downloadAdFiles(Ad ad, Delegate delegate) {
        List<FileSpec> files = ad.getDownloadableFiles(appContext);
        if (files.size() > 0) {
            downloadFiles(files, false, delegate);
        } else {
            if (delegate != null)
                delegate.onFinish(true);
        }
    }

    void deleteAdFiles(Ad ad, Delegate delegate) {
        List<FileSpec> files = ad.getDownloadableFiles(appContext);
        ResanaLog.d(TAG, "deleteAdFiles: " + Arrays.toString(files.toArray()));
        if (files.size() > 0) {
            deleteFiles(files, delegate);
        } else {
            if (delegate != null)
                delegate.onFinish(true);
        }
    }

    void downloadFile(FileSpec file, boolean replaceIfExists, Delegate delegate) {
        final List<FileSpec> files = new ArrayList<>();
        files.add(file);
        downloadFiles(files, replaceIfExists, delegate);
    }

    void downloadFiles(List<FileSpec> files, boolean replaceIfExists, Delegate delegate) {
        downloadFiles(files, replaceIfExists, delegate, downloadExecutor);
    }

    void downloadFiles(List<FileSpec> files, boolean replaceIfExists, Delegate delegate, Executor executor) {
        ResanaLog.d(TAG, "downloadFiles() called with: files = [" + Arrays.toString(files.toArray()) + "], replaceIfExists = [" + replaceIfExists + "], delegate = [" + delegate + "], executor = [" + executor + "]");
        new DownloadFiles(appContext, replaceIfExists, delegate).executeOnExecutor(executor, (FileSpec[]) files.toArray(new FileSpec[files.size()]));
    }

    void pruneFiles(String dirName, final String regex, List<String> allowed, Delegate delegate) {
        ResanaLog.d(TAG, "pruneFiles: ");
        final File dir = appContext.getDir(dirName, Context.MODE_PRIVATE);
        List<FileSpec> toDelete = new ArrayList<>();
        final String[] fNames = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regex);
            }
        });
        for (String fName : fNames) {
            if (fName != null && !allowed.contains(fName))
                toDelete.add(new FileSpec(fName));
        }
        deleteFiles(toDelete, delegate);
    }

    void deleteFile(FileSpec file, Delegate delegate) {
        ResanaLog.d(TAG, "deleteFile: ");
        final List<FileSpec> files = new ArrayList<>();
        files.add(file);
        deleteFiles(files, delegate);
    }

    void deleteFiles(List<FileSpec> files, Delegate delegate) {
        ResanaLog.d(TAG, "deleteFiles: ");
        new DeleteFile(appContext, delegate).executeOnExecutor(commonExecutor, (FileSpec[]) files.toArray(new FileSpec[files.size()]));
    }

    void persistObjectToFile(Object object, FileSpec file, Delegate delegate) {
        ResanaLog.d(TAG, "persistObjectToFile: ");
        new PersistObjectToFile(file.getFile(appContext), object, delegate).executeOnExecutor(commonExecutor);
    }

    void loadObjectFromFile(FileSpec file, Delegate delegate) {
        ResanaLog.d(TAG, "loadObjectFromFile: ");
        List<FileSpec> files = new ArrayList<>();
        files.add(file);
        loadObjectsFromFile(files, delegate);
    }

    void loadObjectsFromFile(List<FileSpec> files, Delegate delegate) {
        ResanaLog.d(TAG, "loadObjectsFromFile: ");
        new LoadObjectsFromFile(appContext, delegate).executeOnExecutor(commonExecutor, (FileSpec[]) files.toArray(new FileSpec[files.size()]));
    }

    void loadBitmapFromFile(FileSpec file, Delegate delegate) {
        ResanaLog.d(TAG, "loadBitmapFromFile: ");
        new LoadBitmap(appContext, delegate).from(file).executeOnExecutor(commonExecutor);
    }

    void loadBitmapFromUrl(String url, Delegate delegate) {
        ResanaLog.d(TAG, "loadBitmapFromUrl: ");
        new LoadBitmap(appContext, delegate).from(url).executeOnExecutor(commonExecutor);
    }

    void loadMovieFromFile(FileSpec file, Delegate delegate) {
        ResanaLog.d(TAG, "loadMovieFromFile: ");
        new LoadMovie(appContext, delegate).from(file).executeOnExecutor(commonExecutor);
    }

    void loadMovieFromUrl(String url, Delegate delegate) {
        ResanaLog.d(TAG, "loadMovieFromUrl: ");
        new LoadMovie(appContext, delegate).from(url).executeOnExecutor(commonExecutor);
    }

    static class FileSpec {
        private static File cacheDir;

        static final int DIR_TYPE_CACHE = 0;
        static final int DIR_TYPE_APKS = 1;

        Integer dir;
        String url;
        String name;
        String tempName;
        String checksum;

        FileSpec(String name) {
            this(null, DIR_TYPE_CACHE, name);
        }

        FileSpec(int dir, String name) {
            this(null, dir, name);
        }

        FileSpec(String url, String name) {
            this(url, DIR_TYPE_CACHE, name);
        }

        FileSpec(String url, int dir, String name) {
            this.url = url;
            this.dir = dir;
            this.name = name;
        }

        void setHasTempName(boolean b) {
            if (b)
                tempName = "temp_" + ((int) (Math.random() * 9999999));
            else
                tempName = null;
        }

        void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        File getFile(Context c) {
            return new File(getDir(c), name);
        }

        File getTempFile(Context c) {
            if (tempName != null)
                return new File(getDir(c), tempName);
            return null;
        }

        private File getDir(Context c) {
            if (dir == DIR_TYPE_CACHE)
                return getCacheDir(c);
//            else if (dir == Dir.APKS_DIR)
            return StorageManager.getApksDir(c);
        }

        private File getCacheDir(Context c) {
            if (cacheDir == null)
                cacheDir = c.getDir(RESANA_CACHE_DIR, Context.MODE_PRIVATE);
            return cacheDir;
        }

        @Override
        public String toString() {
            return "FileSpec{" +
                    "dir=" + dir +
                    ", url='" + url + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    private static void flushAll(Flushable... items) {
        for (Flushable item : items) {
            if (item != null)
                try {
                    item.flush();
                } catch (Exception ignored) {
                }
        }
    }

    private static void closeAll(Closeable... items) {
        for (Closeable item : items) {
            if (item != null)
                try {
                    item.close();
                } catch (Exception ignored) {

                }
        }
    }

    /**
     * persistable objects are objects needed to be saved on disk.
     * every time an object is modified, by calling {@link PersistableObject#onPersist()} method we can save it to file.
     *
     * @param <T> object that needed to be saved on disk.
     */
    static abstract class PersistableObject<T> {
        T object;
        private boolean isPersisting;
        private boolean needsPersist;

        public PersistableObject(T object) {
            this.object = object;
        }

        T get() {
            return object;
        }

        /**
         * saving object to disk.
         * should call onPersisted when finished
         */
        abstract void onPersist();

        void needsPersist() {
            needsPersist = true;
        }

        /**
         * forcely persist the object
         */
        void persist() {
            needsPersist = true;
            persistIfNeeded();
        }

        /**
         * save objects to disk if it is essential
         */
        void persistIfNeeded() {
            if (needsPersist && !isPersisting) {
                isPersisting = true;
                needsPersist = false;
                onPersist();
            }
        }

        /**
         * saving object on the file finished.
         */
        void onPersisted() {
            isPersisting = false;
            persistIfNeeded();
        }

        static class FilePersistedDelegate extends Delegate {
            PersistableObject persistable;

            FilePersistedDelegate(PersistableObject persistable) {
                this.persistable = persistable;
            }

            @Override
            void onFinish(boolean success, Object... args) {
                persistable.onPersisted();
            }
        }
    }

    private static class DownloadFiles extends AsyncTask<FileSpec, Void, Boolean> {
        Delegate delegate;
        Context appContext;
        boolean replaceIfExists;

        DownloadFiles(Context context, boolean replaceIfExists, Delegate delegate) {
            ResanaLog.d(TAG, "DownloadFiles");
            this.delegate = delegate;
            this.replaceIfExists = replaceIfExists;
            this.appContext = context.getApplicationContext();
        }

        @Override
        protected Boolean doInBackground(FileSpec... files) {
            ResanaLog.d(TAG, "DownloadFiles.doInBackground:  " + Arrays.toString(files));
            boolean result = true;
            for (FileSpec file : files) {
                result = downloadFile(file);
                ResanaLog.d(TAG, "DownloadFiles.doInBackground: downloadFile " + file + "    result=" + result);
                if (!result)
                    break;
            }
            return result;
        }

        private boolean downloadFile(FileSpec f) {
            HttpURLConnection connection = null;
            InputStream is = null;
            FileOutputStream fos = null;
            File file = null;
            File tempFile = null;
            try {
                file = f.getFile(appContext);
                if (!replaceIfExists && file.exists())
                    return true;

                connection = NetworkHelper.openConnection(f.url);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return false; // expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
                }

                tempFile = f.getTempFile(appContext);

                is = connection.getInputStream();
                fos = new FileOutputStream(tempFile != null ? tempFile : file);

                MessageDigest md5checksum = null;
                if (f.checksum != null)
                    try {
                        md5checksum = MessageDigest.getInstance("MD5");
                    } catch (Exception e) {
                        ResanaLog.w(TAG, "checking file checksum ignored because of a problem", e);
                    }

                byte data[] = new byte[4096];
                int count;
                while ((count = is.read(data)) != -1) {
                    fos.write(data, 0, count);
                    if (md5checksum != null)
                        md5checksum.update(data, 0, count);
                }
                fos.flush();
                boolean checksumOk = true;
                if (md5checksum != null) {
                    String checksum = new BigInteger(1, md5checksum.digest()).toString(16);
                    checksumOk = f.checksum.equals(checksum);
                    ResanaLog.d(TAG, "downloaded file checksum:" + checksum + (checksumOk ? " is OK" : " does not match source checksum:" + f.checksum));
                }
                if (checksumOk) {
                    if (tempFile != null) {
                        if (tempFile.renameTo(file))
                            return true;
                    } else {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null)
                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {

                    }
                flushAll(fos);
                closeAll(fos, is);
            }
            //clean up files if operation was not successful
            try {
                if (tempFile != null)
                    tempFile.delete();
            } catch (Exception ignored) {
            }
            try {
                if (file != null)
                    file.delete();
            } catch (Exception ignored) {
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ResanaLog.d(TAG, "DownloadFiles.onPostExecute:    success=" + success);
            if (delegate != null)
                delegate.onFinish(success);
        }
    }

    private static class CleanupOldFiles extends AsyncTask<Void, Void, Void> {
        Context appContext;

        CleanupOldFiles(Context context) {
            ResanaLog.d(TAG, "CleanupOldFiles ");
            this.appContext = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ResanaLog.d(TAG, "CleanupOldFiles.doInBackground");
            deleteRecursive(appContext.getDir(RESANA_CACHE_DIR + "2.2.1", Context.MODE_PRIVATE));
            for (int i = 1; i < DB_VERSION; i++) {
                deleteRecursive(appContext.getDir(RESANA_CACHE_DIR_PREF + i, Context.MODE_PRIVATE));
                remove(appContext, PREF_SHOULD_CLEANUP_OLD_FILES + i);
            }
            return null;
        }

        private void deleteRecursive(File file) {
            try {
                if (!file.exists())
                    return;
                if (file.isDirectory())
                    for (File f : file.listFiles())
                        deleteRecursive(f);
                file.delete();
            } catch (Exception e) {
                ResanaLog.w(TAG, "problem in deleting old files", e);
            }
        }
    }

    private static class DeleteFile extends AsyncTask<FileSpec, Void, Boolean> {
        Context appContext;
        Delegate delegate;

        public DeleteFile(Context context, Delegate delegate) {
            ResanaLog.d(TAG, "DeleteFile");
            this.delegate = delegate;
            this.appContext = context.getApplicationContext();
        }

        @Override
        protected Boolean doInBackground(FileSpec... files) {
            ResanaLog.d(TAG, "DeleteFiles.doInBackground: " + Arrays.toString(files));
            boolean success = true;
            for (FileSpec f : files) {
                final File file = f.getFile(appContext);
                if (file.exists() && !file.delete())
                    success = false;
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ResanaLog.d(TAG, "DeleteFile.onPostExecute: success=" + success);
            if (delegate != null)
                delegate.onFinish(success);
        }
    }

    private static class PersistObjectToFile extends AsyncTask<Void, Void, Boolean> {
        Delegate delegate;
        File file;
        Object object;

        PersistObjectToFile(File f, Object o, Delegate delegate) {
            ResanaLog.d(TAG, "PersistObjectToFile");
            this.file = f;
            this.object = o;
            this.delegate = delegate;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            ResanaLog.d(TAG, "PersistObjectToFile.doInBackground");
            FileOutputStream fos = null;
            ObjectOutputStream os = null;
            try {
                fos = new FileOutputStream(file);
                os = new ObjectOutputStream(fos);
                os.writeObject(object);
                return true;
            } catch (IOException ignored) {

            } finally {
                flushAll(fos, os);
                closeAll(fos, os);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ResanaLog.d(TAG, "PersistObjectToFile.onPostExecute: success=" + success);
            if (delegate != null)
                delegate.onFinish(success);
        }
    }

    private static class LoadObjectsFromFile extends AsyncTask<FileSpec, Void, Boolean> {
        Object[] objects;
        Context appContext;
        Delegate delegate;

        LoadObjectsFromFile(Context context, Delegate delegate) {
            ResanaLog.d(TAG, "LoadObjectsFromFile");
            this.appContext = context.getApplicationContext();
            this.delegate = delegate;
        }

        @Override
        protected Boolean doInBackground(FileSpec... files) {
            ResanaLog.d(TAG, "LoadObjectsFromFile.doInBackground: " + Arrays.toString(files));
            objects = new Object[files.length];
            for (int i = 0; i < files.length; i++)
                objects[i] = loadObject(files[i]);

            for (Object o : objects)
                if (o == null)
                    return false;
            return true;
        }

        Object loadObject(FileSpec f) {
            FileInputStream fis = null;
            ObjectInputStream is = null;
            try {
                final File file = f.getFile(appContext);
                if (file.exists()) {
                    fis = new FileInputStream(file);
                    is = new ObjectInputStream(fis);
                    return is.readObject();
                }
            } catch (IOException | ClassNotFoundException ignored) {

            } finally {
                closeAll(fis, is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ResanaLog.d(TAG, "LoadObjectsFromFile.onPostExecute: success=" + success);
            if (delegate != null)
                delegate.onFinish(success, objects);
        }
    }

    private static class LoadBitmap extends AsyncTask<Void, Void, Boolean> {
        Delegate delegate;
        Context appContext;
        Bitmap bitmap;

        FileSpec file;
        String url;

        LoadBitmap(Context context, Delegate delegate) {
            ResanaLog.d(TAG, "LoadBitmap");
            this.appContext = context.getApplicationContext();
            this.delegate = delegate;
        }

        LoadBitmap from(FileSpec file) {
            this.file = file;
            return this;
        }

        LoadBitmap from(String url) {
            this.url = url;
            return this;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            ResanaLog.d(TAG, "LoadBitmap.doInBackground:   file=" + file + "   url=" + url);
            if (file != null)
                loadBitmap(file);
            else
                loadBitmap(url);
            return bitmap != null;
        }

        void loadBitmap(FileSpec f) {
            final File file = f.getFile(appContext);
            ResanaLog.d(TAG, "LoadBitmap.loadBitmap: fromFile f=" + f + "   exists=" + file.exists());
            if (file.exists())
                try {
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                } catch (Throwable ignored) {

                }
        }

        void loadBitmap(String url) {
            ResanaLog.d(TAG, "LoadBitmap.loadBitmap: fromUrl    url=" + url);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
            } catch (Throwable ignored) {

            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ResanaLog.d(TAG, "LoadBitmap.onPostExecute: " + "file=" + file + "  url=" + url + " success=" + success + "   bitmap=" + bitmap);
            if (delegate != null)
                delegate.onFinish(success, bitmap);
        }
    }

    private static class LoadMovie extends AsyncTask<Void, Void, Boolean> {
        Delegate delegate;
        Context appContext;
        Movie movie;

        FileSpec file;
        String url;

        LoadMovie(Context context, Delegate delegate) {
            ResanaLog.d(TAG, "LoadMovie");
            this.appContext = context.getApplicationContext();
            this.delegate = delegate;
        }

        LoadMovie from(FileSpec file) {
            this.file = file;
            return this;
        }

        LoadMovie from(String url) {
            this.url = url;
            return this;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            ResanaLog.d(TAG, "LoadMovie.doInBackground:   file=" + file + "   url=" + url);
            if (file != null)
                loadMovie(file);
            else
                loadMovie(url);
            return movie != null;
        }

        void loadMovie(FileSpec f) {
            final File file = f.getFile(appContext);
            ResanaLog.d(TAG, "LoadMovie.loadMovie: fromFile f=" + f + "   exists=" + file.exists());
            if (file.exists())
                try {
                    movie = Movie.decodeStream(new FileInputStream(file));
                } catch (Throwable ignored) {

                }
        }

        void loadMovie(String url) {
            ResanaLog.d(TAG, "LoadMovie.loadMovie: fromUrl    url=" + url);
            try {
                movie = Movie.decodeStream(new URL(url).openConnection().getInputStream());
            } catch (Throwable ignored) {

            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            ResanaLog.d(TAG, "LoadMovie.onPostExecute: " + "file=" + file + "  url=" + url + " success=" + aBoolean + "   movie=" + movie);
            delegate.onFinish(aBoolean, movie);
        }
    }
}