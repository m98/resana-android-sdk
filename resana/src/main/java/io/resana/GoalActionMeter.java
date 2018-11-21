package io.resana;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.util.Map;
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
        cleanupPrefs();
        checkPendingReports();
        sendPendingAcks();
    }

    void persistReport(String id, ReportDto r) {
        if (r == null)
            return;
        String key = "r_" + id;
        prefs.edit()
                .putString(key, DtoParser.toString(r))
                .putLong("t_" + key, System.currentTimeMillis())
                .apply();
    }

    void checkReport(String id) {
        String key = "r_" + id;
        final String r = prefs.getString(key, null);
        if (r != null) {
            try {
                checkReport(DtoParser.parse(r, ReportDto.class));
            } catch (Exception ignored) {
            }
        }
    }

    void checkReport(ReportDto r) {
        if (r == null)
            return;
        String key = "pt_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 100));
        prefs.edit()
                .putString(key, DtoParser.toString(r))
                .apply();
        createCheckReportTask(key, r, true);
    }

    private void cleanupPrefs() {
        final Map<String, ?> map = prefs.getAll();
        final SharedPreferences.Editor editor = prefs.edit();
        for (String key : map.keySet()) {
            if (key.startsWith("pt_")) {
                try {
                    final ReportDto report = DtoParser.parse((String) map.get(key), ReportDto.class);
                    long time = Long.valueOf(key.split("_")[1]);
                    if (System.currentTimeMillis() - time > report.ttl * 1000)
                        editor.remove(key);
                } catch (Exception ignored) {
                }
            } else if (key.startsWith("r_")) {
                try {
                    final ReportDto report = DtoParser.parse((String) map.get(key), ReportDto.class);
                    long time = (Long) map.get("t_" + key);
                    if (System.currentTimeMillis() - time > report.ttl * 1000)
                        editor.remove(key).remove("t_" + key);
                } catch (Exception ignored) {
                }
            }
        }
        editor.apply();
    }

    private void checkPendingReports() {
        final Map<String, ?> map = prefs.getAll();
        for (String key : map.keySet()) {
            if (key.startsWith("pt_")) {
                try {
                    createCheckReportTask(key, DtoParser.parse((String) map.get(key), ReportDto.class), false);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void sendPendingAcks() {
        final Map<String, ?> map = prefs.getAll();
        for (String key : map.keySet()) {
            if (key.startsWith("pa_")) {
                prefs.edit().remove(key).apply();
                sendReportAckToServer((String) map.get(key));
            }
        }
    }

    private void removePendingReport(String key) {
        prefs.edit()
                .remove(key)
                .apply();
    }

    private void sendReportToServer(ReportDto report) {
        if (report.type == ReportDto.REPORT_TYPE_INSTALL)
            sendReportAckToServer("R_INS_pkg:" + report.param);
        else
            sendReportAckToServer("R_TJCH_tch:" + report.param);
    }

    private void sendReportAckToServer(String r) {
        final ResanaInternal resana = ResanaInternal.instance;
        if (resana != null)
            resana.sendToServer(r);
        else
            prefs.edit()
                    .putString("pa_" + ((int) (Math.random() * 10000)), r)
                    .apply();
    }

    private void createCheckReportTask(String key, ReportDto r, boolean wait) {
        new CheckReportTask(appContext, key, r, wait).executeOnExecutor(executor);
    }

    private static class CheckReportTask extends AsyncTask<Void, Void, Boolean> {
        Context appContext;
        String key;
        ReportDto report;
        boolean wait;

        CheckReportTask(Context context, String key, ReportDto report, boolean wait) {
            this.appContext = context.getApplicationContext();
            this.key = key;
            this.report = report;
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
                    if (ApkManager.getInstance(appContext).isApkInstalled(report.param))
                        return true;
                }
                return false;
            } else
                return ApkManager.getInstance(appContext).isApkInstalled(report.param);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success)
                GoalActionMeter.getInstance(appContext).sendReportToServer(report);
            GoalActionMeter.getInstance(appContext).removePendingReport(key);
        }
    }
}