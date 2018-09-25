package io.resana;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class NetworkHelper {
    private static volatile String deviceUserAgent;

    static HttpURLConnection openConnection(String url) throws IOException {
        return openConnection("GET", url, null, null);
    }

    static HttpURLConnection openConnection(String method, String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        ResanaLog.d("NetworkHelper", "openConnection() method = [" + method + "], url = [" + url + "], headers = [" + headers + "], params = [" + params + "]");
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

    static class UrlNotSupportedException extends IllegalStateException {
        UrlNotSupportedException(String s) {
            super(s);
        }
    }
}