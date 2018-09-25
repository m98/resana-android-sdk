package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class LandingDto implements Parcelable, Serializable {
    static final int IMAGE = 0;
    static final int VIDEO = 1;

    @SerializedName("id")
    long id;

    @Mandatory
    @SerializedName("url")
    String url;

    @Mandatory
    @SerializedName("mimeType")
    String type;

    @SerializedName("chs")
    String checksum;

    @SerializedName("w")
    int width = 0;

    @SerializedName("h")
    int height = 0;


    private LandingDto(Parcel in) {
        id = in.readLong();
        url = in.readString();
        type = in.readString();
        checksum = in.readString();
        width = in.readInt();
        height = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(url);
        dest.writeString(type);
        dest.writeString(checksum);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public static final Creator<LandingDto> CREATOR = new Creator<LandingDto>() {
        @Override
        public LandingDto createFromParcel(Parcel in) {
            return new LandingDto(in);
        }

        @Override
        public LandingDto[] newArray(int size) {
            return new LandingDto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    String getFileName() {
        return "visual_" + id;
    }

    int getLandingType() {
        if (type.equals("image/jpeg")
                || type.equals("image/png"))
            return NativeAd.IMAGE;
        if (type.equals("video/mpeg-4"))//todo more look at here
            return NativeAd.VIDEO;
        if (type.equals("text/html"))
            return NativeAd.WEB_PAGE;
        if (type.equals("image/gif"))
            return NativeAd.GIF;
        return -1;
    }
}
