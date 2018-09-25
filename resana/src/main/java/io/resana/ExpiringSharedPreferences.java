package io.resana;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ExpiringSharedPreferences implements SharedPreferences {
    private SharedPreferences prefs;
    private long defExpiresAfterMillis;
    private List<WeakReference<OnSharedPreferenceChangeListener>> listeners = new ArrayList<>();
    private OnSharedPreferenceChangeListener internalListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final Iterator<WeakReference<OnSharedPreferenceChangeListener>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                final OnSharedPreferenceChangeListener listener = iterator.next().get();
                if (listener != null)
                    listener.onSharedPreferenceChanged(ExpiringSharedPreferences.this, key);
                else
                    iterator.remove();
            }
        }
    };

    ExpiringSharedPreferences(Context context, String sharedPreferenceName, long defExpireAfterMillis) {
        checkArgs(context, sharedPreferenceName, defExpireAfterMillis);
        this.prefs = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE);
        this.defExpiresAfterMillis = defExpireAfterMillis;
        this.prefs.registerOnSharedPreferenceChangeListener(internalListener);
        cleanup();
    }

    private void checkArgs(Context context, String sharedPreferencesName, long defExpiresAfterMillis) {
        if (context == null)
            throw new IllegalArgumentException("context can not be null");
        if (sharedPreferencesName == null || sharedPreferencesName.isEmpty())
            throw new IllegalArgumentException("sharedPreferencesName can not be null neither empty");
        if (defExpiresAfterMillis <= 0)
            throw new IllegalArgumentException("defExpiresAfterMillis must be a greater than zero");
    }

    private void cleanup() {
        getAll();
    }

    @Override
    public Map<String, ?> getAll() {
        final Map<String, ?> all = prefs.getAll();
        final Map<String, Object> result = new HashMap<>();
        Object v;
        for (String key : all.keySet()) {
            v = getValueIfNotExpired(key);
            if (v != null)
                result.put(key, v);
        }
        return result;
    }

    @Override
    public String getString(String key, String defValue) {
        final Object v = getValueIfNotExpired(key);
        if (v != null)
            return v.toString();
        return defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        final Object v = getValueIfNotExpired(key);
        if (v != null && v instanceof JSONArray)
            try {
                final JSONArray ja = ((JSONArray) v);
                final Set<String> result = new HashSet<>();
                for (int i = 0; i < ja.length(); i++)
                    result.add(ja.getString(i));
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        final Object v = getValueIfNotExpired(key);
        if (v != null)
            try {
                return v instanceof Integer ? ((Integer) v) : Integer.valueOf(v.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        final Object v = getValueIfNotExpired(key);
        if (v != null)
            try {
                return v instanceof Long ? ((Long) v) : Long.valueOf(v.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        final Object v = getValueIfNotExpired(key);
        if (v != null)
            try {
                return v instanceof Float ? ((Float) v) : Float.valueOf(v.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        final Object v = getValueIfNotExpired(key);
        if (v != null)
            try {
                return v instanceof Boolean ? ((Boolean) v) : Boolean.valueOf(v.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        final Object v = getValueIfNotExpired(key);
        return v != null;
    }

    public Editor edit() {
        return new Editor(prefs.edit());
    }

    private Object getValueIfNotExpired(String key) {
        final String s = prefs.getString(key, null);
        if (s != null)
            try {
                final JSONObject jo = new JSONObject(s);
                final long expiration = jo.getLong("t");
                if (System.currentTimeMillis() < expiration)
                    return jo.get("v");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        edit().remove(key).apply();
        return null;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        //unregister this object if currently is registered
        unregisterOnSharedPreferenceChangeListener(listener);
        listeners.add(new WeakReference<>(listener));
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        final Iterator<WeakReference<OnSharedPreferenceChangeListener>> itr = listeners.iterator();
        while (itr.hasNext()) {
            final WeakReference<OnSharedPreferenceChangeListener> next = itr.next();
            if (next.get() == listener)
                itr.remove();
        }
    }

    public class Editor implements SharedPreferences.Editor {
        SharedPreferences.Editor editor;

        public Editor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public Editor putString(String key, String value) {
            return putString(key, value, defExpiresAfterMillis);
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            return putStringSet(key, values, defExpiresAfterMillis);
        }

        @Override
        public Editor putInt(String key, int value) {
            return putInt(key, value, defExpiresAfterMillis);
        }

        @Override
        public Editor putLong(String key, long value) {
            return putLong(key, value, defExpiresAfterMillis);
        }

        @Override
        public Editor putFloat(String key, float value) {
            return putFloat(key, value, defExpiresAfterMillis);
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return putBoolean(key, value, defExpiresAfterMillis);
        }

        public Editor putString(String key, String value, long expiresAfterMillis) {
            if (value == null)
                remove(key);
            else if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor putStringSet(String key, Set<String> value, long expiresAfterMillis) {
            if (value == null)
                remove(key);
            else if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor putInt(String key, int value, long expiresAfterMillis) {
            if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor putLong(String key, long value, long expiresAfterMillis) {
            if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor putFloat(String key, float value, long expiresAfterMillis) {
            if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor putBoolean(String key, boolean value, long expiresAfterMillis) {
            if (expiresAfterMillis > 0)
                editor.putString(key, getTimeIncludedValue(value, expiresAfterMillis));
            return this;
        }

        public Editor remove(String key) {
            editor.remove(key);
            return this;
        }

        public Editor clear() {
            editor.clear();
            return this;
        }

        public boolean commit() {
            return editor.commit();
        }

        public void apply() {
            editor.apply();
        }
    }

    private String getTimeIncludedValue(Object value, long expiresAfterMillis) {
        try {
            final JSONObject jo = new JSONObject();
            jo.put("t", System.currentTimeMillis() + expiresAfterMillis);
            if (value instanceof Set) {
                final JSONArray ja = new JSONArray();
                for (Object item : ((Set) value))
                    ja.put(item);
                jo.put("v", ja);
            } else
                jo.put("v", value);
            return jo.toString();
        } catch (JSONException e) {
            throw new RuntimeException(
                    "Problem occured while trying to create JSONObject for value="
                            + value + " expiresAfterMillis=" + expiresAfterMillis, e);
        }
    }
}
