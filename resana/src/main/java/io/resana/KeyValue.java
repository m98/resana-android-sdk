/******************************************************************************
 * Copyright 2015-2016 Befrest
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

class KeyValue implements Parcelable, Serializable {
    @Mandatory
    private String k;
    @Mandatory
    private String v;

    public KeyValue(String key, String value) {
        this.k = key;
        this.v = value;
    }

    public String getKey() {
        return k;
    }

    public String getValue() {
        return v;
    }

    @Override
    public String toString() {
        return "KeyValue{" +
                "name='" + k + '\'' +
                ", value='" + v + '\'' +
                '}';
    }

    private KeyValue(Parcel in) {
        k = in.readString();
        v = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(k);
        dest.writeString(v);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<KeyValue> CREATOR = new Creator<KeyValue>() {
        @Override
        public KeyValue createFromParcel(Parcel in) {
            return new KeyValue(in);
        }

        @Override
        public KeyValue[] newArray(int size) {
            return new KeyValue[size];
        }
    };
}