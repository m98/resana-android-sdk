package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


class SplashDto extends AdDto implements Parcelable, Serializable {
    @SerializedName("pic")
    @Mandatory
    String pic;

    @SerializedName("pcolor")
    String progressColor;

    @SerializedName("dr")
    int duration = -1;

    private SplashDto(Parcel in) {
        super(in);
        pic = in.readString();
        progressColor = in.readString();
        duration = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(pic);
        dest.writeString(progressColor);
        dest.writeInt(duration);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SplashDto> CREATOR = new Creator<SplashDto>() {
        @Override
        public SplashDto createFromParcel(Parcel in) {
            return new SplashDto(in);
        }

        @Override
        public SplashDto[] newArray(int size) {
            return new SplashDto[size];
        }
    };
}
