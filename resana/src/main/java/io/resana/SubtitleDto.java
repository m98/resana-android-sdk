package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * subtitle ads are deprecated and will be removed as soon as december 1,2018
 */
@Deprecated
class SubtitleDto extends AdDto implements Parcelable, Serializable {
    @SerializedName("m")
    @Base64
    @Mandatory
    String m;

    @Mandatory
    @SerializedName("tcolor")
    String fontColor;

    @SerializedName("logo")
    String logo;

    @SerializedName("pic")
    String pic;

    private SubtitleDto(Parcel in) {
        super(in);
        m = in.readString();
        logo = in.readString();
        fontColor = in.readString();
        pic = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(m);
        dest.writeString(logo);
        dest.writeString(fontColor);
        dest.writeString(pic);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SubtitleDto> CREATOR = new Creator<SubtitleDto>() {
        @Override
        public SubtitleDto createFromParcel(Parcel in) {
            return new SubtitleDto(in);
        }

        @Override
        public SubtitleDto[] newArray(int size) {
            return new SubtitleDto[size];
        }
    };
}
