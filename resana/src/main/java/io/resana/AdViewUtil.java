/******************************************************************************
 * Copyright 2015-2016 ResanaInternal
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.resana;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AdViewUtil {
    private static final String TAG = ResanaLog.TAG_PREF + "AdViewUtil";
    private static final int DEFAULT_BG_ALPHA = 200;

    private static final String MEDIA_ID = "ResanaMediaId";
    static final String IS_TEL_CLIENT = "Resana_isTelegramClient";
    private static Typeface tf;
    private static final String RESANA_FONT = "Resana-Font.ttf";
    private static final String RESANA_FONT_PATH = "fonts";
    private static Executor commonExecutor;

    static long getNeededTimeForAnimatingText(TextView tv) {
        Context context = tv.getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        return ((tv.getWidth() / Math.min(d.getWidth(), d.getHeight())) + 1) * 10000;
    }

    static void runOnceWhenViewWasDrawn(final View view, final Runnable runnable) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                runnable.run();
            }
        });
    }

    static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * displayMetrics.density);
        return px;
    }

    public static int parseArgbColor(String rawColor) {
        try {
            String[] color = rawColor.split(",");
            if (color.length == 3) {
                String[] tmp = new String[4];
                tmp[0] = "" + AdViewUtil.DEFAULT_BG_ALPHA;
                tmp[1] = color[0];
                tmp[2] = color[1];
                tmp[3] = color[2];
                color = tmp;
            }
            return Color.argb(Integer.parseInt(color[0]), Integer.parseInt(color[1]),
                    Integer.parseInt(color[2]), Integer.parseInt(color[3]));
        } catch (Exception e) {
            ResanaLog.d(TAG, "Could not parse color: " + rawColor);
            return Color.BLACK;
        }
    }

    public static int parseHexadecimalColor(String rawColor, int defColor) {
        //a malformed color causes ad to be assumed corrupted on versions 1.x.x
        try {
            if (rawColor.length() == 4)// #fff -->  #ffffff
                rawColor = rawColor + rawColor.substring(1);
            return Color.parseColor(rawColor);
        } catch (Exception e) {
            ResanaLog.d(TAG, "Could not parse color: " + rawColor);
            return defColor;
        }
    }

    public static void setTypeFace(Context context, TextView tv) { //todo maybe we want font back
//        ResanaLog.d(TAG, "setTypeFace");
        try {
//            if (tf == null)
//                tf = Typeface.createFromAsset(context.getAssets(), RESANA_FONT_PATH + File.separator + RESANA_FONT);
//            tv.setTypeface(tf);
        } catch (Exception ignored) {
            String s;
        }
    }

    static String getResizedImageUrl(String imgUrl) {
        final DisplayMetrics display = Resources.getSystem().getDisplayMetrics();
        final int w = display.widthPixels;
        final int h = display.heightPixels;
        return imgUrl + "/resize/" + w + "x" + h;
    }

    static int getResanaLabelMaxWidth() {
        return (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.3);
    }

    static String getMediaId(Context appContext) {
        try {
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            int mediaIdAsInt = bundle.getInt(MEDIA_ID, -1);
            if (mediaIdAsInt != -1)
                return String.valueOf(mediaIdAsInt);
            else
                return bundle.getString(MEDIA_ID);
        } catch (PackageManager.NameNotFoundException e) {
            ResanaLog.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        }
        return null;
    }

    static Intent getAdActionIntent(Ad ad) {
        Intent intent = parseIntentString(ad.data.intent);
        if (intent == null && ad.getLink() != null) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(ad.getLink()));
        }
        return intent;
    }

    static Intent parseIntentString(String intent) {
        if (intent != null && intent.length() > 0)
            try {
                return Intent.parseUri(intent, Intent.URI_INTENT_SCHEME);
            } catch (Exception ignored) {
            }
        return null;
    }

    static Executor getCommonExecutor() {
        if (commonExecutor == null)
            commonExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ResanaThreadFactory("Resana_CommonPool"));
        return commonExecutor;
    }

    static void writeStringMapToParcel(Parcel out, Map<String, String> map) {
        if (map == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                out.writeString(entry.getKey());
                out.writeString(entry.getValue());
            }
        }
    }

    static Map<String, String> readStringMapFromParcel(Parcel in) {
        int size = in.readInt();
        if (size < 0)
            return null;
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++)
            map.put(in.readString(), in.readString());
        return map;
    }
}

