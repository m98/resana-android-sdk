package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Text implements Parcelable, Serializable {

    @SerializedName("sh")
    String shortText;

    @SerializedName("md")
    String mediumText;

    protected Text(Parcel in) {
        shortText = in.readString();
        mediumText = in.readString();
    }

    public static final Creator<Text> CREATOR = new Creator<Text>() {
        @Override
        public Text createFromParcel(Parcel in) {
            return new Text(in);
        }

        @Override
        public Text[] newArray(int size) {
            return new Text[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(shortText);
        dest.writeString(mediumText);
    }
}
