package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class ReportDto implements Parcelable, Serializable {
    static final int REPORT_TYPE_INSTALL = 0;
    static final int REPORT_TYPE_TEL_JOIN = 1;

    @Mandatory
    @SerializedName("t")
    @NumericValues({REPORT_TYPE_INSTALL})
    Integer type;

    @Mandatory
    @SerializedName("param")
    String param;

    int ttl = 60 * 60; //1hour (sec)

    private ReportDto(Parcel in) {
        type = in.readInt();
        param = in.readString();
        ttl = in.readInt();
    }

    public static final Creator<ReportDto> CREATOR = new Creator<ReportDto>() {
        @Override
        public ReportDto createFromParcel(Parcel in) {
            return new ReportDto(in);
        }

        @Override
        public ReportDto[] newArray(int size) {
            return new ReportDto[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(param);
        dest.writeInt(ttl);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}