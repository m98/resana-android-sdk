package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class ControlDto implements Parcelable, Serializable {
    static final String CMD_COOL_DOWN = "cd";
    static final String CMD_FLUSH = "flush";
    static final String CMD_RESANA_LABEL = "rl";
    static final String CMD_DISMISS_OPTIONS = "dmo";
    static final String CMD_BLOCKED_ZONES = "bz";
    static final String CMD_LAST_MODIFIED_DATE = "lmd";

    /**
     * old commands:
     * rht : resanaHelpText
     */

    String cmd;
    Params params;

    private ControlDto() {
    }

    static ControlDto[] parseArray(JSONArray array) {
        ArrayList<ControlDto> res = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                res.add(fromJson(array.getJSONObject(i)));
            } catch (Exception ignored) {
            }
        }
        if (res.size() < 1)
            throw new RuntimeException("invalid ctrl array!");
        return res.toArray(new ControlDto[0]);
    }

    /**
     * this functions converts json to a controlDto class
     *
     * @param jo json objects needs to be parsed to controlDto class
     * @return controlDto
     * @throws JSONException
     */
    private static ControlDto fromJson(JSONObject jo) throws JSONException {
        final ControlDto res = new ControlDto();
        res.cmd = jo.getString("cmd");
        if (CMD_FLUSH.equals(res.cmd)) {
            //no params
        } else if (CMD_COOL_DOWN.equals(res.cmd)) {
            res.params = CoolDownParams.fromJson(jo.getJSONObject("params"));
        } else if (CMD_RESANA_LABEL.equals(res.cmd)) {
            res.params = ResanaLabelParams.fromJson(jo.getJSONObject("params"));
        } else if (CMD_DISMISS_OPTIONS.equals(res.cmd)) {
            res.params = DismissOptionsParams.fromJson(jo.getJSONObject("params"));
        } else if (CMD_BLOCKED_ZONES.equals(res.cmd)) {
            res.params = BlockedZonesParams.fromJsonArray(jo.getJSONArray("bz"));
        } else if (CMD_LAST_MODIFIED_DATE.equals(res.cmd)) {//todo should handle here
//            res.params = LastModifiedDateParams.fromJson(jo.getJSONObject("lm"));
        } else
            throw new RuntimeException("invalid cmd!");
        return res;
    }

    protected ControlDto(Parcel in) {
        cmd = in.readString();
        params = in.readParcelable(Params.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cmd);
        dest.writeParcelable(params, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ControlDto> CREATOR = new Creator<ControlDto>() {
        @Override
        public ControlDto createFromParcel(Parcel in) {
            return new ControlDto(in);
        }

        @Override
        public ControlDto[] newArray(int size) {
            return new ControlDto[size];
        }
    };

    @Override
    public String toString() {
        return "ControlDto{" +
                "cmd='" + cmd + '\'' +
                ", params=" + params +
                '}';
    }

    static class Params implements Parcelable, Serializable {
        private Params() {
        }

        protected Params(Parcel in) {
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Params> CREATOR = new Creator<Params>() {
            @Override
            public Params createFromParcel(Parcel in) {
                return new Params(in);
            }

            @Override
            public Params[] newArray(int size) {
                return new Params[size];
            }
        };
    }

    static class CoolDownParams extends Params implements Parcelable, Serializable {
        Subtitle subtitle;
        Chance splash;
        Chance nativeAd;

        private CoolDownParams() {
        }

        static CoolDownParams fromJson(JSONObject jo) throws JSONException {
            CoolDownParams res = new CoolDownParams();
            if (jo.has("sub"))
                res.subtitle = Subtitle.fromJson(jo.getJSONObject("sub"));
            if (jo.has("splash"))
                res.splash = Chance.fromJson(jo.getJSONObject("splash"));
            if (jo.has("native"))
                res.nativeAd = Chance.fromJson(jo.getJSONObject("native"));
            if (res.subtitle == null && res.splash == null && res.nativeAd == null)
                throw new RuntimeException("invalid cooldown param!");
            return res;
        }

        protected CoolDownParams(Parcel in) {
            super(in);
            subtitle = in.readParcelable(Subtitle.class.getClassLoader());
            splash = in.readParcelable(Chance.class.getClassLoader());
            nativeAd = in.readParcelable(Chance.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(subtitle, flags);
            dest.writeParcelable(splash, flags);
            dest.writeParcelable(nativeAd, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<CoolDownParams> CREATOR = new Creator<CoolDownParams>() {
            @Override
            public CoolDownParams createFromParcel(Parcel in) {
                return new CoolDownParams(in);
            }

            @Override
            public CoolDownParams[] newArray(int size) {
                return new CoolDownParams[size];
            }
        };

        @Override
        public String toString() {
            return "CoolDownParams{" +
                    "subtitle=" + subtitle +
                    ", splash=" + splash +
                    ", native=" + nativeAd +
                    "} ";
        }

        static class Subtitle implements Parcelable, Serializable {
            //mandatory
            int min;
            int max;
            int first;

            //optional
            double chance = 1;
            int ttl = -1;
            double firstChance = -1; //f
            int interval = -1;

            private Subtitle() {
            }

            static Subtitle fromJson(JSONObject jo) throws JSONException {
                Subtitle res = new Subtitle();
                res.min = jo.getInt("min");
                res.max = jo.getInt("max");
                res.first = jo.getInt("first");
                if (jo.has("ttl"))
                    res.ttl = jo.getInt("ttl");
                if (jo.has("chance"))
                    res.chance = jo.getDouble("chance");
                if (jo.has("f"))
                    res.firstChance = jo.getDouble("f");
                if (jo.has("i"))
                    res.interval = jo.getInt("i");
                if (res.min < 0 || res.max < 0 || res.first < 0 || res.min > res.max || res.chance < 0 || res.chance > 1 || res.firstChance > 1)
                    throw new RuntimeException("incorrect values!");
                return res;
            }

            protected Subtitle(Parcel in) {
                min = in.readInt();
                max = in.readInt();
                first = in.readInt();
                chance = in.readDouble();
                ttl = in.readInt();
                firstChance = in.readDouble();
                interval = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(min);
                dest.writeInt(max);
                dest.writeInt(first);
                dest.writeDouble(chance);
                dest.writeInt(ttl);
                dest.writeDouble(firstChance);
                dest.writeInt(interval);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Subtitle> CREATOR = new Creator<Subtitle>() {
                @Override
                public Subtitle createFromParcel(Parcel in) {
                    return new Subtitle(in);
                }

                @Override
                public Subtitle[] newArray(int size) {
                    return new Subtitle[size];
                }
            };

            @Override
            public String toString() {
                return "Subtitle{" +
                        "min=" + min +
                        ", max=" + max +
                        ", first=" + first +
                        ", chance=" + chance +
                        ", ttl=" + ttl +
                        ", firstChance=" + firstChance +
                        ", interval=" + interval +
                        '}';
            }
        }

        static class Chance implements Parcelable, Serializable {
            //mandatory
            double chance;

            //optional
            int ttl = -1;
            double first = -1; //f
            int interval = -1; //i

            private Chance() {
            }

            static Chance fromJson(JSONObject jo) throws JSONException {
                Chance res = new Chance();
                res.chance = jo.getDouble("chance");
                if (jo.has("ttl"))
                    res.ttl = jo.getInt("ttl");
                if (jo.has("f"))
                    res.first = jo.getDouble("f");
                if (jo.has("i"))
                    res.interval = jo.getInt("i");
                if (res.chance < 0 || res.chance > 1 || res.first > 1)
                    throw new RuntimeException("incorrect values!");
                return res;
            }

            protected Chance(Parcel in) {
                chance = in.readDouble();
                ttl = in.readInt();
                first = in.readDouble();
                interval = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeDouble(chance);
                dest.writeInt(ttl);
                dest.writeDouble(first);
                dest.writeInt(interval);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Chance> CREATOR = new Creator<Chance>() {
                @Override
                public Chance createFromParcel(Parcel in) {
                    return new Chance(in);
                }

                @Override
                public Chance[] newArray(int size) {
                    return new Chance[size];
                }
            };

            @Override
            public String toString() {
                return "Chance{" +
                        "chance=" + chance +
                        ", ttl=" + ttl +
                        ", first=" + first +
                        ", interval=" + interval +
                        '}';
            }
        }
    }

    static class ResanaLabelParams extends Params implements Parcelable, Serializable {
        String label; //l
        String text; //t

        private ResanaLabelParams() {
        }

        static ResanaLabelParams fromJson(JSONObject jo) throws JSONException {
            final ResanaLabelParams res = new ResanaLabelParams();
            res.label = jo.getString("l");
            res.text = BefrestImpl.Util.decodeBase64(jo.getString("t"));
            return res;
        }

        protected ResanaLabelParams(Parcel in) {
            super(in);
            label = in.readString();
            text = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(label);
            dest.writeString(text);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<ResanaLabelParams> CREATOR = new Creator<ResanaLabelParams>() {
            @Override
            public ResanaLabelParams createFromParcel(Parcel in) {
                return new ResanaLabelParams(in);
            }

            @Override
            public ResanaLabelParams[] newArray(int size) {
                return new ResanaLabelParams[size];
            }
        };

        @Override
        public String toString() {
            return "ResanaLabelParams{" +
                    "label='" + label + '\'' +
                    ", text='" + text + '\'' +
                    "} ";
        }
    }

    static class DismissOptionsParams extends Params implements Serializable, Parcelable {
        //mandatory
        boolean dismissible; //e

        //optional
        Map<String, String> options; //o
        int restDuration; //rd


        DismissOptionsParams() {
        }

        static DismissOptionsParams fromJson(JSONObject jo) throws JSONException {
            final DismissOptionsParams res = new DismissOptionsParams();
            res.dismissible = jo.getBoolean("e");
            if (jo.has("o")) {
                res.options = new HashMap<>();
                JSONObject options = jo.getJSONObject("o");
                Iterator<String> itr = options.keys();
                String key;
                while (itr.hasNext()) {
                    key = itr.next();
                    res.options.put(key, BefrestInternal.Util.decodeBase64(options.getString(key)));
                }
                if (res.options.size() == 0)
                    res.options = null;
            }
            if (jo.has("rd"))
                res.restDuration = jo.getInt("rd");
            return res;
        }

        protected DismissOptionsParams(Parcel in) {
            super(in);
            dismissible = in.readByte() != 0;
            restDuration = in.readInt();
            options = AdViewUtil.readStringMapFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (dismissible ? 1 : 0));
            dest.writeInt(restDuration);
            AdViewUtil.writeStringMapToParcel(dest, options);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<DismissOptionsParams> CREATOR = new Creator<DismissOptionsParams>() {
            @Override
            public DismissOptionsParams createFromParcel(Parcel in) {
                return new DismissOptionsParams(in);
            }

            @Override
            public DismissOptionsParams[] newArray(int size) {
                return new DismissOptionsParams[size];
            }
        };
    }

    static class BlockedZonesParams extends Params implements Serializable, Parcelable {
        String[] zones;

        private BlockedZonesParams() {

        }

        static BlockedZonesParams fromJsonArray(JSONArray jsonArray) {
            if (jsonArray.length() == 0)
                return null;
            final BlockedZonesParams res = new BlockedZonesParams();
            res.zones = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    res.zones[i] = jsonArray.getString(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return res;
        }

        protected BlockedZonesParams(Parcel in) {
            super(in);
            zones = in.createStringArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(zones);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<BlockedZonesParams> CREATOR = new Creator<BlockedZonesParams>() {
            @Override
            public BlockedZonesParams createFromParcel(Parcel source) {
                return new BlockedZonesParams(source);
            }

            @Override
            public BlockedZonesParams[] newArray(int size) {
                return new BlockedZonesParams[size];
            }
        };
    }

    static class LastModifiedDateParams extends Params implements Serializable, Parcelable {
        long modifiedHour;

//        static LastModifiedDateParams fromJson(JSONObject jsonObject) {
//            LastModifiedDateParams res = new LastModifiedDateParams();
////            res.modifiedHour =
//        }
    }
}