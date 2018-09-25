package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

class SimulateClickDto implements Parcelable, Serializable {
    static final int ON_RECEIVE = 0;
    static final int ON_ACK = 1;
    static final int ON_CLICK = 2;
    static final int ON_LANDING_CLICK = 3;

    @Mandatory
    @SerializedName("u")
    String url;

    @Mandatory
    @NumericValues({ON_RECEIVE, ON_ACK, ON_CLICK, ON_LANDING_CLICK})
    @SerializedName("w")
    Integer when;

    @Mandatory
    @SerializedName("m")
    String method;

    @SerializedName("h")
    Map<String, String> headers;

    @SerializedName("p")
    Map<String, String> params;

    private SimulateClickDto(Parcel in) {
        url = in.readString();
        when = in.readInt();
        method = in.readString();
        headers = AdViewUtil.readStringMapFromParcel(in);
        params = AdViewUtil.readStringMapFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeInt(when);
        dest.writeString(method);
        AdViewUtil.writeStringMapToParcel(dest, headers);
        AdViewUtil.writeStringMapToParcel(dest, params);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SimulateClickDto> CREATOR = new Creator<SimulateClickDto>() {
        @Override
        public SimulateClickDto createFromParcel(Parcel in) {
            return new SimulateClickDto(in);
        }

        @Override
        public SimulateClickDto[] newArray(int size) {
            return new SimulateClickDto[size];
        }
    };

    private transient int internalHashCode; //custom

    @Override
    public int hashCode() {
        if (internalHashCode == 0)
            internalHashCode = new Gson().toJson(this).hashCode();
        return internalHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof SimulateClickDto)
            return hashCode() == obj.hashCode();
        return false;
    }
}