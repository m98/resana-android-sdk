package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class TextDto implements Parcelable, Serializable {

    @SerializedName("ord")
    Text ordinaryText;

    @SerializedName("title")
    Text titleText;

    protected TextDto(Parcel in) {
        ordinaryText = in.readParcelable(Text.class.getClassLoader());
        titleText = in.readParcelable(Text.class.getClassLoader());
    }

    public static final Creator<TextDto> CREATOR = new Creator<TextDto>() {
        @Override
        public TextDto createFromParcel(Parcel in) {
            return new TextDto(in);
        }

        @Override
        public TextDto[] newArray(int size) {
            return new TextDto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(ordinaryText, flags);
        dest.writeParcelable(titleText, flags);
    }
}
