package io.resana;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.resana.FileManager.Delegate;

class NetworkManager {
    static final String BASE_URL = "";
    static final String NATIVE_URL = "http://172.30.24.84:6644/app/10073/ad/type?type=native";//todo make it real later
    static final String CONTROL_URL = "";

    private static volatile String deviceUserAgent;
    private static final String TAG = ResanaLog.TAG_PREF + "NetworkManager";

    private static NetworkManager instance;

    private Executor getResponseExecutor;

    private NetworkManager() {
        getResponseExecutor = new ThreadPoolExecutor(0, 1, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new ResanaThreadFactory("Resana_NM_RPool"));
    }

    static NetworkManager getInstance() {
        NetworkManager localInstance = instance;
        if (localInstance == null) {
            synchronized (NetworkManager.class) {
                localInstance = instance;
                if (localInstance == null)
                    localInstance = instance = new NetworkManager();
            }
        }
        return localInstance;
    }

    static HttpURLConnection openConnection(String url) throws IOException {
        return openConnection("GET", url, null, null);
    }

    static HttpURLConnection openConnection(String method, String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        ResanaLog.d("NetworkManager", "openConnection() method = [" + method + "], url = [" + url + "], headers = [" + headers + "], params = [" + params + "]");
        HttpURLConnection connection;
        int responseCode;
        CookieManager cookieManager = new CookieManager();
        if (headers == null)
            headers = new HashMap<>();
        if (!headers.containsKey("User-Agent") && deviceUserAgent != null)
            headers.put("User-Agent", deviceUserAgent);
        do {
            if (!url.startsWith("http") && !url.startsWith("https"))
                throw new UrlNotSupportedException("non http destination");
            String cookies = getCookiesHeaderForUrl(cookieManager, url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setInstanceFollowRedirects(true); //does not follow redirects from a protocol to another (e.g. http to https and vice versa)
            connection.setRequestMethod(method);
            if (!TextUtils.isEmpty(cookies))
                connection.setRequestProperty("Cookie", cookies);
            for (Map.Entry<String, String> entry : headers.entrySet())
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            if ("POST".equals(method) && params != null && !params.isEmpty()) {
                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(getQuery(params).getBytes());
                outputStream.flush();
                outputStream.close();
            }
            responseCode = connection.getResponseCode();
            addCookiesToCookieManager(cookieManager, url, connection.getHeaderFields().get("Set-Cookie"));
            url = connection.getHeaderField("Location");
        } while (responseCode / 100 == 3 && !TextUtils.isEmpty(url));
        return connection;
    }

    private static void addCookiesToCookieManager(CookieManager cookieManager, String url, List<String> cookies) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ignored) {
        }
        if (cookies != null)
            for (String cookie : cookies)
                try {
                    cookieManager.getCookieStore().add(uri, HttpCookie.parse(cookie).get(0));
                } catch (Exception ignored) {
                }
    }

    private static String getCookiesHeaderForUrl(CookieManager cookieManager, String url) {
        String s = "";
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ignored) {
        }
        if (uri != null) {
            for (HttpCookie cookie : cookieManager.getCookieStore().get(uri))
                s += cookie.getName() + "=" + cookie.getValue() + ";";
        }
        if (TextUtils.isEmpty(s))
            return null;
        else
            return s.substring(0, s.length() - 1);
    }

    private static String getQuery(Map<String, String> params) {
        String result = "";
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result += "&";
            try {
                result += URLEncoder.encode(entry.getKey(), "UTF-8");
                result += "=";
                result += URLEncoder.encode(entry.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        return result;
    }

    static void checkUserAgent(final Context context) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Locale l = context.getResources().getConfiguration().locale;

//                    WebView webView = new WebView(context);
//                    deviceUserAgent = webView.getSettings().getUserAgentString();
//                    webView.destroy();

                    deviceUserAgent = System.getProperty("http.agent");

                    //fix locale  https://stackoverflow.com/questions/40398528/android-webview-language-changes-abruptly-on-android-n
                    fixLocale(context, l);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void fixLocale(Context context, Locale l) {
                if (!context.getResources().getConfiguration().locale.equals(l)) {
                    Locale.setDefault(l);
                    Configuration conf = context.getResources().getConfiguration();
                    conf.locale = l;
                    context.getResources().updateConfiguration(conf, context.getResources().getDisplayMetrics());
                }
            }
        });
    }

    private static String getResponseFromUrl(String url, String method, Map<String, String> headers, Map<String, String> params) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(method, url, headers, params);
            connection.connect();
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                ResanaLog.e(TAG, "getResponseFromUrl: unable to get response from url " + url + ". error code=" + status);
                return null;
            }
            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    private String generateReportUrl(String type, String adId) {
        String mediaId = ResanaInternal.mediaId;
        return BASE_URL + "/api/" + mediaId + "/report/" + type + "/" + adId;
    }

    void getNativeAds(Delegate delegate, String... zone) {
        new GetAds(delegate).executeOnExecutor(getResponseExecutor, NATIVE_URL);
    }

    void getNativeAds(Delegate delegate) {
        ResanaLog.d(TAG, "getNativeAds:");
        getNativeAds(delegate, null);
    }

    void sendReports(String type, String adId) {
        new PutReport(type, adId, null).execute(type, adId);
    }

    private static class GetAds extends AsyncTask<String, Void, List<Ad>> {
        Delegate delegate;

        GetAds(Delegate delegate) {
            ResanaLog.d(TAG, "GetAds");
            this.delegate = delegate;
        }

        @Override
        protected List<Ad> doInBackground(String... strings) {
            List<Ad> ads = new ArrayList<>();
            String url = strings[0];
            ResanaLog.d(TAG, "GetAds.doInBackground:  url=" + url);
            String rawMsg = getResponseFromUrl(url, "GET", null, null);
            if (rawMsg == null)
                return null;
            ResanaLog.e(TAG, "doInBackground: rawMsg=" + rawMsg);
            try {
                JSONArray adsArray = new JSONObject(rawMsg).getJSONObject("entity").getJSONArray("ads");
                for (int i = 0; i < adsArray.length(); i++) {
                    Ad ad = new Ad(adsArray.get(i).toString());
                    ads.add(ad);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return ads;
        }

        @Override
        protected void onPostExecute(List<Ad> ads) {
            if (delegate != null) {
                if (ads == null)
                    delegate.onFinish(false);
                else delegate.onFinish(true, ads);
            }
        }
    }

    private static class PutReport extends AsyncTask<String, Void, Boolean> {
        String type;
        String adId;
        Delegate delegate;

        PutReport(String type, String adId, Delegate delegate) {
            ResanaLog.d(TAG, "PutReport");
            this.type = type;
            this.adId = adId;
            this.delegate = delegate;
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String url = NetworkManager.getInstance().generateReportUrl(type, adId);
            ResanaLog.d(TAG, "PutReport.doInBackground: put to " + url);
            Map<String, String> headers = new HashMap<>();
            headers.put("X-RS-DEVICE-ID", ResanaInternal.deviceId);
            String rawMsg = getResponseFromUrl(url, "PUT", headers, null);//todo get response to check error code
            return null;
        }
    }

    static class UrlNotSupportedException extends IllegalStateException {
        UrlNotSupportedException(String s) {
            super(s);
        }
    }
}