package io.resana;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class GoalActionMeter {
    private final static String REPORTS_PREF = "RESANA_REPORTS_29871";
    private static GoalActionMeter instance;
    private Context appContext;
    private SharedPreferences prefs;
    private Executor executor;

    static GoalActionMeter getInstance(Context context) {
        GoalActionMeter localInstance = instance;
        if (localInstance == null) {
            synchronized (GoalActionMeter.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new GoalActionMeter(context);
                }
            }
        }
        return localInstance;
    }

    private GoalActionMeter(Context context) {
        appContext = context.getApplicationContext();
        prefs = context.getSharedPreferences(REPORTS_PREF, Context.MODE_PRIVATE);
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ResanaThreadFactory("Resana_GAM_Pool")
        );
    }

    void checkInstall(String adId, String pkName) {
        new CheckReportTask(appContext, adId, pkName, true).executeOnExecutor(executor);
    }

    private static class CheckReportTask extends AsyncTask<Void, Void, Boolean> {
        private Context appContext;
        private String adId;
        private String pkName;
        private boolean wait;

        CheckReportTask(Context context, String adId, String pkName, boolean wait) {
            this.appContext = context.getApplicationContext();
            this.adId = adId;
            this.pkName = pkName;
            this.wait = wait;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (wait) {
                for (int i = 0; i < 6; i++) {
                    try {
                        Thread.sleep(60 * 1000);//one minute
                    } catch (InterruptedException ignored) {
                    }
                    if (ApkManager.getInstance(appContext).isApkInstalled(pkName))
                        return true;
                }
                return false;
            } else
                return ApkManager.getInstance(appContext).isApkInstalled(pkName);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) //todo handle fail reports
                NetworkManager.getInstance().sendReports(NativeAdProvider.Reports.install, adId);
        }
    }
}