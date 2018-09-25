package io.resana.campaigntracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class ResanaCampaignTracker {
    private static final String PREF = "ResanaCampaignTracker";
    private static final String PREF_REQUEST_SENT = "PREF_REQUEST_SENT";

    private boolean isSending;

    private Context appContext;

    public ResanaCampaignTracker(Context context) {
        appContext = context.getApplicationContext();
        track();
    }

    public void track() {
        if (!isSending && !requestSentBefore())
            sendTrackRequest();
    }

    private void sendTrackRequest() {
        isSending = true;
        final String uid = DeviceCredentials.getDeviceUuid(appContext).toString();
        final String pkg = appContext.getPackageName();
        new Request().execute(uid, pkg);
    }

    private boolean requestSentBefore() {
        return appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(PREF_REQUEST_SENT, false);
    }

    private void requestSentSuccessfully() {
        appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_REQUEST_SENT, true).apply();
    }

    private static class DeviceCredentials {
        private static final String PREFS_FILE = "dev";
        private static final String PREFS_DEVICE_ID = "device_id";
        private static UUID uuid;

        public static UUID getDeviceUuid(Context context) {
            if (uuid == null) {
                synchronized (DeviceCredentials.class) {
                    if (uuid == null) {
                        final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
                        final String id = prefs.getString(PREFS_DEVICE_ID, null);

                        if (id != null) {
                            // Use the ids previously computed and stored in the prefs file
                            uuid = UUID.fromString(id);

                        } else {

                            final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                            // Use the Android ID unless it's broken, in which case fallback on deviceId,
                            // unless it's not available, then fallback on a random number which we store
                            // to a prefs file
                            try {
                                if (!"9774d56d682e549c".equals(androidId)) {
                                    uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                                } else {
                                    final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                                    uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                                }
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }

                            // Write the value out to the prefs file
                            prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).apply();
                        }
                    }
                }
            }
            return uuid;
        }

    }

    private class Request extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... values) {
            HttpsURLConnection conn = null;
            OutputStream os = null;
            OutputStreamWriter osw = null;
            BufferedWriter writer = null;
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try {
                conn = (HttpsURLConnection) new URL("https://xapi.resana.io/app/v1/report/installation").openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                List<NameValuePair> params = new ArrayList<>();
                params.add(new NameValuePair("did", values[0]));
                params.add(new NameValuePair("pkg", values[1]));

                os = conn.getOutputStream();
                osw = new OutputStreamWriter(os, "UTF-8");
                writer = new BufferedWriter(osw);
                writer.write(getQuery(params));
                writer.flush();

                conn.connect();

                final int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK)
                    return false;

                is = conn.getInputStream();
                isr = new InputStreamReader(is);
                reader = new BufferedReader(isr);
                String s = "";
                String line;
                while ((line = reader.readLine()) != null)
                    s += line;
                final JSONObject jsonObject = new JSONObject(s);
                return jsonObject.getInt("errorCode") == 0;
            } catch (Exception ignored) {
            } finally {
                try {
                    if (conn != null)
                        conn.disconnect();
                } catch (Exception ignored) {
                }
                flushAll(writer, osw, os);
                closeAll(writer, osw, os, reader, isr, is);
            }
            return false;
        }

        private void flushAll(Flushable... items) {
            for (Flushable item : items) {
                try {
                    item.flush();
                } catch (IOException ignored) {
                }
            }
        }

        private void closeAll(Closeable... items) {
            for (Closeable item : items) {
                try {
                    item.close();
                } catch (Exception ignored) {
                }
            }
        }

        private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (NameValuePair pair : params) {
                if (first)
                    first = false;
                else
                    result.append("&");
                result.append(URLEncoder.encode(pair.name, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.value, "UTF-8"));
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success)
                requestSentSuccessfully();
        }
    }

    private static class NameValuePair {
        String name;
        String value;

        NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
