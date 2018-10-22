package io.resana;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class VisualDto implements Parcelable, Serializable {

    @SerializedName("org")
    LandingDto org;

    @SerializedName("sq")
    LandingDto sq;

    @SerializedName("hrz")
    LandingDto hrz;

    protected VisualDto(Parcel in) {
        org = in.readParcelable(LandingDto.class.getClassLoader());
        sq = in.readParcelable(LandingDto.class.getClassLoader());
        hrz = in.readParcelable(LandingDto.class.getClassLoader());
    }

    public static final Creator<VisualDto> CREATOR = new Creator<VisualDto>() {
        @Override
        public VisualDto createFromParcel(Parcel in) {
            return new VisualDto(in);
        }

        @Override
        public VisualDto[] newArray(int size) {
            return new VisualDto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(org, flags);
        dest.writeParcelable(sq, flags);
        dest.writeParcelable(hrz, flags);
    }
}