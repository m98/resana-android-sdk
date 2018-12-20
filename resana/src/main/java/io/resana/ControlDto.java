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
    @SerializedName("ttl")
    int controlsTTL;
    @SerializedName("rl")
    String resanaLabel;
    @SerializedName("bz")
    String[] blockedZones;

    protected ControlDto(Parcel in) {
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
        dest.writeInt(controlsTTL);
        dest.writeString(resanaLabel);
        dest.writeStringArray(blockedZones);
    }
}