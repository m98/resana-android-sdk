package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class ControlDto implements Parcelable, Serializable {

    public static final Creator<ControlDto> CREATOR = new Creator<ControlDto>() {
        @Override
        public ControlDto createFromParcel(Parcel in) {
            return new ControlDto(in);
        }

        @Override
        public ControlDto[] newArray(int size) {
            return new ControlDto[size];
        }
    };
    @SerializedName("ch0")
    int nativeChance = 1;
    @SerializedName("ch1")
    int splashChance = 1;
    @SerializedName("ttl")
    int controlsTTL = 1000;
    @SerializedName("rl")
    String resanaLabel = ResanaInternal.DEFAULT_RESANA_INFO_TEXT;
    @SerializedName("bz")
    String[] blockedZones;
    @SerializedName("ch2")
    int videoStickyChance = 1;

    protected ControlDto(Parcel in) {
        nativeChance = in.readInt();
        splashChance = in.readInt();
        videoStickyChance = in.readInt();
        controlsTTL = in.readInt();
        resanaLabel = in.readString();
        blockedZones = in.createStringArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(nativeChance);
        dest.writeInt(splashChance);
        dest.writeInt(videoStickyChance);
        dest.writeInt(controlsTTL);
        dest.writeString(resanaLabel);
        dest.writeStringArray(blockedZones);
    }
}