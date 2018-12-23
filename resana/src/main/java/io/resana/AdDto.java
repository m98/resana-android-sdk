package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

abstract class AdDto implements Parcelable, Serializable {
    static final int AD_TYPE_SUBTITLE = 0;
    static final int AD_TYPE_BAD_TYPE = 1; //type 1 should not be used due to a bug in primitive versions
    static final int AD_TYPE_SPLASH = 2;
    static final int AD_TYPE_NATIVE = 3;
    static final int AD_TYPE_VIDEO_STICKY = 6;
    static final int AD_TYPE_STATIC_BANNER = 4; //todo not handled yet

    @Mandatory
    @NumericValues({AD_TYPE_SPLASH, AD_TYPE_NATIVE, AD_TYPE_VIDEO_STICKY})
    @SerializedName("type")
    Integer type;

    @SerializedName("id")
    @Mandatory
    Long id;

    @SerializedName("bg")
    String backgroundColor;

    @SerializedName("cfa")
    String callForAction;

    @SerializedName("z")
    String[] zones;

    @SerializedName("ver")
    int version = -1;

    @SerializedName("link")
    String link;

    @SerializedName("intent")
    String intent;

    @SerializedName("lnd")
    LandingDto landing;

    @SerializedName("report")
    ReportDto report;

    @SerializedName("apk")
    ApkDto apk;

    @SerializedName("rl")
    ResanaLabelDto resanaLabel;


    @SerializedName("mv")
    int maxView = 1000;

    @SerializedName("ttl")
    int ttl = -1;

    @SerializedName("ctl")
    int ctl = 1;

    @SerializedName("hot")
    boolean hot;

    @SerializedName("flags")
    int flags = 0;

    protected AdDto(Parcel in) {
        type = in.readInt();
        id = in.readLong();
        backgroundColor = in.readString();
        callForAction = in.readString();
        zones = in.createStringArray();
        version = in.readInt();
        link = in.readString();
        intent = in.readString();
        landing = in.readParcelable(LandingDto.class.getClassLoader());
        report = in.readParcelable(ReportDto.class.getClassLoader());
        apk = in.readParcelable(ApkDto.class.getClassLoader());
        resanaLabel = in.readParcelable(ResanaLabelDto.class.getClassLoader());
        maxView = in.readInt();
        ttl = in.readInt();
        ctl = in.readInt();
        hot = in.readByte() != 0;
        flags = in.readInt();
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeLong(id);
        dest.writeString(backgroundColor);
        dest.writeString(callForAction);
        dest.writeStringArray(zones);
        dest.writeInt(version);
        dest.writeString(link);
        dest.writeString(intent);
        dest.writeParcelable(landing, flags);
        dest.writeParcelable(report, flags);
        dest.writeParcelable(apk, flags);
        dest.writeParcelable(resanaLabel, flags);
        dest.writeInt(maxView);
        dest.writeInt(ttl);
        dest.writeInt(ctl);
        dest.writeByte((byte) (hot ? 1 : 0));
        dest.writeInt(flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
