package io.resana;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.Serializable;

public class ApkDto implements Parcelable, Serializable {

    static final int NETWORK_TYPE_WIFI_ONLY = 0;
    static final int NETWORK_TYPE_ANY = 1;
    @Mandatory
    @SerializedName("pkg")
    String pkg;
    @SerializedName("url")
    String url = "";
    @Mandatory
    @SerializedName("net")
    @NumericValues({NETWORK_TYPE_WIFI_ONLY, NETWORK_TYPE_ANY})
    Integer net;
    @SerializedName("chs")
    String checksum;
    @SerializedName("v")
    int version = 0;
    @SerializedName("pt")
    int prepareTime = 0;


    protected ApkDto(Parcel in) {
        pkg = in.readString();
        url = in.readString();
        if (in.readByte() == 0) {
            net = null;
        } else {
            net = in.readInt();
        }
        checksum = in.readString();
        version = in.readInt();
        prepareTime = in.readInt();
    }

    public static final Creator<ApkDto> CREATOR = new Creator<ApkDto>() {
        @Override
        public ApkDto createFromParcel(Parcel in) {
            return new ApkDto(in);
        }

        @Override
        public ApkDto[] newArray(int size) {
            return new ApkDto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pkg);
        dest.writeString(url);
        if (net == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(net);
        }
        dest.writeString(checksum);
        dest.writeInt(version);
        dest.writeInt(prepareTime);
    }

    String getApkFileName() {
        return pkg + "_" + version;
    }

    File getApkFile(Context context) {
        return new File(StorageManager.getApksDir(context), getApkFileName());
    }
}
