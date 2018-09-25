package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class ResanaLabelDto implements Parcelable, Serializable {
    @Mandatory
    @SerializedName("l")
    String label;

    @Mandatory
    @Base64
    @SerializedName("t")
    String text;

    private ResanaLabelDto(Parcel in) {
        label = in.readString();
        text = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(text);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ResanaLabelDto> CREATOR = new Creator<ResanaLabelDto>() {
        @Override
        public ResanaLabelDto createFromParcel(Parcel in) {
            return new ResanaLabelDto(in);
        }

        @Override
        public ResanaLabelDto[] newArray(int size) {
            return new ResanaLabelDto[size];
        }
    };
}
