/******************************************************************************
 * Copyright 2015-2016 Befrest
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.resana;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static io.resana.FileManager.CUSTOM_LABEL_PREF;
import static io.resana.FileManager.FileSpec;
import static io.resana.FileManager.LANDING_IMAGE_PREF;
import static io.resana.FileManager.NATIVE_FILE_NAME_PREFIX;
import static io.resana.FileManager.SPLASH_FILE_NAME_PREFIX;
import static io.resana.FileManager.SUBTITLE_FILE_NAME_PREFIX;

final class Ad implements Parcelable, Serializable {
    private static final String TAG = ResanaLog.TAG_PREF + "AdMessage";

    boolean isCorrupted;
    AdDto data;
    ControlDto[] ctrls;
    int renderedCount;

    //SubtitleView state saving specific parameters
    long textAnimCurrentPlayTime;
    long imageShowingTimeElapsed;

    Ad(String rawMsg) {
        long time = System.currentTimeMillis();
        try {
            final Object o = (new JSONTokener(rawMsg)).nextValue();
            if (o instanceof JSONObject) {
                data = parseAdDto(rawMsg, (JSONObject) o);
            }
            else
                throw new RuntimeException("invalid!");
        } catch (Exception e) { //JSONException or any other unExpected Exception
            isCorrupted = true;
            ResanaLog.w(TAG, "Could not parse a message", e);
        }
    }

    private AdDto parseAdDto(String raw, JSONObject jo) throws JSONException {
        final int type = jo.getInt("type");
        switch (type) {
            case AdDto.AD_TYPE_SPLASH:
                return DtoParser.parse(raw, SplashDto.class);
            case AdDto.AD_TYPE_NATIVE:
                return DtoParser.parse(raw, NativeDto.class);
            case AdDto.AD_TYPE_BAD_TYPE:
                throw new RuntimeException("Can not parse Ad. bad type!");
            default:
                throw new RuntimeException("Can not parse Ad. unknown type!");
        }
    }

    boolean isControlMsg() {
        return ctrls != null;
    }

    boolean hasLanding() {
        return data.landing != null;
    }

    boolean isInvalid(Context context) {
        Log.e(TAG, "isInvalid: isOldVersion: " + AdVersionKeeper.isOldVersion(this)
         + " isRenderedEnough: " + AdVersionKeeper.isRenderedEnough(this)
         + "ApkInstalled: " + ApkManager.getInstance(context).isApkInstalled(this));
        return AdVersionKeeper.isOldVersion(this)
                || AdVersionKeeper.isRenderedEnough(this)
                || ApkManager.getInstance(context).isAdInvalid(this);
    }

    boolean hasApk() {
        return data.apk != null && data.apk.url != null && !data.apk.url.equals("");
    }

    boolean hasPackageName() {
        return data.apk != null && data.apk.pkg != null && !data.apk.pkg.equals("");
    }

    String getPackageName() {
        if (hasPackageName())
            return data.apk.pkg;
        return "";
    }

    String getApkUrl() {
        if (hasApk())
            return data.apk.url;
        return null;
    }

    Integer getType() {
        if (data != null)
            return data.type;
        return -1;
    }

    String getOrder() {
        return "" + data.id;
    }

    String getId() {
        return data.id + "_" + data.version;
    }

    String getLink() {
        return data.link;
    }

    String getImgUrl() {
        if (getType() == AdDto.AD_TYPE_SPLASH)
            return AdViewUtil.getResizedImageUrl(((SplashDto) data).pic);
        return null;
    }

    String getLandingImageUrl() {
        if (getType() == AdDto.AD_TYPE_SPLASH)
            return AdViewUtil.getResizedImageUrl(((SplashDto) data).landing.url);
        else if (getType() == AdDto.AD_TYPE_NATIVE)
            return AdViewUtil.getResizedImageUrl(((NativeDto) data).landing.url);
        return null;
    }

    int getBackgroundColor() {
        if (data.backgroundColor != null)
            return AdViewUtil.parseArgbColor(data.backgroundColor);
        return Color.BLACK;
    }

    int getDuration() {
        if (getType() == AdDto.AD_TYPE_SPLASH)
            return ((SplashDto) data).duration;
        else if (getType() == AdDto.AD_TYPE_SUBTITLE)
            return 20 * 1000;//TODO
        return 0;
    }

    String getLandingImageFileName() {
        if (getType() == AdDto.AD_TYPE_SPLASH)
            return SPLASH_FILE_NAME_PREFIX + LANDING_IMAGE_PREF + getId();
        else if (getType() == AdDto.AD_TYPE_SUBTITLE)
            return SUBTITLE_FILE_NAME_PREFIX + LANDING_IMAGE_PREF + getId();
        else if (getType() == AdDto.AD_TYPE_NATIVE)
            return NATIVE_FILE_NAME_PREFIX + LANDING_IMAGE_PREF + getId();
        return null;
    }

    String getInfoText(Context context) {
        if (data.resanaLabel != null)
            return data.resanaLabel.text;
        return ResanaPreferences.getString(context, ResanaPreferences.PREF_RESANA_INFO_TEXT, ResanaInternal.DEFAULT_RESANA_INFO_TEXT);
    }

    String getLabelFileName() {
        if (hasCustomLabel()) {
            if (getType() == AdDto.AD_TYPE_SPLASH)
                return SPLASH_FILE_NAME_PREFIX + CUSTOM_LABEL_PREF + getId();
            else if (getType() == AdDto.AD_TYPE_SUBTITLE)
                return SUBTITLE_FILE_NAME_PREFIX + CUSTOM_LABEL_PREF + getId();
            else if (getType() == AdDto.AD_TYPE_NATIVE)
                return NATIVE_FILE_NAME_PREFIX + CUSTOM_LABEL_PREF + getId();
        } else
            return "resana_label";
        return null;
    }

    boolean hasCustomLabel() {
        return data.resanaLabel != null;
    }

    String getLabelUrl(Context context) {
        if (hasCustomLabel())
            return data.resanaLabel.label;
        return ResanaPreferences.getString(context, ResanaPreferences.PREF_RESANA_INFO_LABEL, "none");
    }

    List<FileSpec> getDownloadableFiles(Context c) {
        List<FileSpec> files = new ArrayList<>();
        if (getType() == AdDto.AD_TYPE_NATIVE) {
            List<Integer> indexes = VisualsManager.getDownloadingVisualsIndex(c, this);
            for (int i = 0; i < data.maxView && i < indexes.size(); i++) { //downloading only max view number of visuals. not all
                int index = indexes.get(i);
                VisualDto v = ((NativeDto) data).visuals.get(index);
                if (ResanaConfig.gettingHrzVisual(c)) {
                    String url = AdViewUtil.getResizedImageUrl(v.hrz.url);
                    FileSpec hrzFile = new FileSpec(url, v.hrz.getFileName());
                    files.add(hrzFile);
                }
                if (ResanaConfig.gettingOrgVisual(c)) {
                    String url = AdViewUtil.getResizedImageUrl(v.org.url);
                    FileSpec orgFile = new FileSpec(url, v.org.getFileName());
                    files.add(orgFile);
                }
                if (ResanaConfig.gettingSqVisual(c)) {
                    String url = AdViewUtil.getResizedImageUrl(v.sq.url);
                    FileSpec sqFile = new FileSpec(url, v.sq.getFileName());
                    files.add(sqFile);
                }
            }
        }
        if (getType() == AdDto.AD_TYPE_SPLASH)
            files.add(new FileSpec(getImgUrl(), getSplashImageFileName()));
        if (hasLanding()) {
            FileSpec landingFile = new FileSpec(getLandingImageUrl(), getLandingImageFileName());
            files.add(landingFile);
        }
        if (hasCustomLabel() && !"none".equals(getLabelUrl(c)))
            files.add(new FileSpec(getLabelUrl(c), getLabelFileName()));
        return files;
    }

    //Splash
    String getSplashImageFileName() {
        return SPLASH_FILE_NAME_PREFIX + getId();
    }

    int getSubtitleProgressColor() {
        return AdViewUtil.parseHexadecimalColor(((SplashDto) data).progressColor, Color.YELLOW);
    }

    @Override
    public String toString() {
        return "Ad{" +
                "isCorrupted=" + isCorrupted +
                ", data=" + data +
                ", ctrls=" + ctrls +
                ", renderedCount=" + renderedCount +
                ", textAnimCurrentPlayTime=" + textAnimCurrentPlayTime +
                ", imageShowingTimeElapsed=" + imageShowingTimeElapsed +
                '}';
    }

    protected Ad(Parcel in) {
        isCorrupted = in.readByte() != 0;
        data = in.readParcelable(AdDto.class.getClassLoader());
        ctrls = in.createTypedArray(ControlDto.CREATOR);
        textAnimCurrentPlayTime = in.readLong();
        imageShowingTimeElapsed = in.readLong();
        renderedCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isCorrupted ? 1 : 0));
        dest.writeParcelable(data, flags);
        dest.writeTypedArray(ctrls, flags);
        dest.writeLong(textAnimCurrentPlayTime);
        dest.writeLong(imageShowingTimeElapsed);
        dest.writeInt(renderedCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Ad> CREATOR = new Creator<Ad>() {
        @Override
        public Ad createFromParcel(Parcel in) {
            return new Ad(in);
        }

        @Override
        public Ad[] newArray(int size) {
            return new Ad[size];
        }
    };

    static class Flags {
        static final int UR_ACTION_ON_IMAGE_CLICK = 0b1;
        static final int SUBTITLE_NOT_SKIPPABLE = 0b10;
        static final int SPLASH_END_OF_VIDEO = 0b100;
    }
}