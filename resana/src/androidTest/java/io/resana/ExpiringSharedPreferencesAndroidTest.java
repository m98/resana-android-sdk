package io.resana;

import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExpiringSharedPreferencesAndroidTest {
    ExpiringSharedPreferences prefs;
    long defaultExpiresAfter = 1000;

    @Before
    public void createPreferences() {
        prefs = new ExpiringSharedPreferences(InstrumentationRegistry.getContext(), "test_expiring_prefs", defaultExpiresAfter);
        prefs.edit().clear().apply();
    }

    @After
    public void destroyPreferences() {
        prefs.edit().clear().apply();
        assertEquals(prefs.getAll().keySet().size(), 0);
    }

    @Test
    public void implementsAndroidSharedPreferences() {
        final ExpiringSharedPreferences.Editor editor = prefs.edit();
        assertThat(prefs, instanceOf(SharedPreferences.class));
        assertThat(editor, instanceOf(SharedPreferences.Editor.class));
    }

    @Test
    public void getAll() {
        int expiresAfter = 2000;
        final int intVal = 913;
        final long longVal = 9328674;
        final float floatVal = -231.6f;
        final ExpiringSharedPreferences.Editor editor = prefs.edit();
        editor.putString("k1", "salam", expiresAfter);
        editor.putBoolean("k2", true, expiresAfter);
        editor.putInt("k3", intVal, expiresAfter);
        editor.putLong("k4", longVal, expiresAfter / 2);
        editor.putFloat("k5", floatVal, expiresAfter / 2);
        editor.apply();

        threadSleep(expiresAfter / 2);
        Map<String, ?> all = prefs.getAll();
        assertEquals(3, all.keySet().size());
        final Object[] actuals = all.keySet().toArray();
        Arrays.sort(actuals);
        assertArrayEquals(new String[]{"k1", "k2", "k3"}, actuals);
        threadSleep(expiresAfter / 2 + 5);
        all = prefs.getAll();
        assertEquals(0, all.keySet().size());
    }

    @Test
    public void stringDefaultExpiration() {
        String key = "key";
        String value = "value مقدار ";
        prefs.edit().putString(key, value).apply();
        assertEquals(value, prefs.getString(key, null));
        threadSleep(defaultExpiresAfter / 5);
        assertEquals(value, prefs.getString(key, null));
        threadSleep(defaultExpiresAfter);
        String defValue = "default";
        assertEquals(defValue, prefs.getString(value, defValue));
    }

    @Test
    public void stringsCustomExpiration() {
        String key = "کلید دوم";
        String value = "second value";
        int time = 100; //ms
        prefs.edit().putString(key, value, time).apply();
        assertEquals(value, prefs.getString(key, null));
        threadSleep(time / 2);
        assertEquals(value, prefs.getString(key, null));
        threadSleep(time / 2 + 5);
        String defValue = "default";
        assertEquals(defValue, prefs.getString(key, defValue));
        Arrays.asList();
    }

    @Test
    public void emptyStringSet() {
        String key = "empty_string_set";
        Set<String> values = new HashSet<>();
        prefs.edit().putStringSet(key, values).commit();
        final Set<String> actual = prefs.getStringSet(key, null);
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    public void stringSetCustomExpiration() {
        String key = "string set";
        Set<String> values = new HashSet<>();
        values.add("gholilksajdfad lksjdf sdflkj");
        values.add("ldjfau مبتنسیمبعهضصخثبا منتسیب منتسیب میبن");
        for (int i = 0; i < 10; i++)
            values.add("عدد شماره " + i);
        long time = 300;
        prefs.edit().putStringSet(key, values, time).apply();
        Set<String> actual = prefs.getStringSet(key, null);
        assertNotNull(actual);
        assertEquals(actual, values);
        assertEquals(actual.size(), values.size());
        threadSleep(time / 2);
        actual = prefs.getStringSet(key, null);
        assertNotNull(actual);
        assertEquals(actual, values);
        assertEquals(actual.size(), values.size());
        threadSleep(time / 2 + 1);
        assertNull(prefs.getStringSet(key, null));
    }

    @Test
    public void stringSetDefaultExpiration() {
        String key = "string set default expiration";
        Set<String> values = new HashSet<>();
        values.add("gholilksajdfad lksjdf sdflkj");
        values.add("ldjfau مبتنسیمبعهضصخثبا منتسیب منتسیب میبن");
        for (int i = 0; i < 10; i++)
            values.add("عدد شماره " + i);
        prefs.edit().putStringSet(key, values).apply();
        Set<String> actual = prefs.getStringSet(key, null);
        assertEquals(values, actual);

        threadSleep(defaultExpiresAfter / 2);
        actual = prefs.getStringSet(key, null);
        assertEquals(actual, values);

        threadSleep(defaultExpiresAfter / 2 + 15);
        assertNull(prefs.getStringSet(key, null));
    }

    @Test
    public void integerDefaultExpiration() {
        String key = "integerDefaultExpiration";
        int value = (int) (Math.random() * Integer.MAX_VALUE);
        prefs.edit().putInt(key, value).apply();
        assertEquals(value, prefs.getInt(key, -1));
        threadSleep(defaultExpiresAfter / 3);
        assertEquals(value, prefs.getInt(key, -1));
        threadSleep(defaultExpiresAfter);
        assertEquals(-5, prefs.getInt(key, -5));
    }

    @Test
    public void integerCustomExpiration() {
        String key = "integerCustomExpiration";
        int value = -(int) (Math.random() * Integer.MAX_VALUE);
        int time = 150;
        prefs.edit().putInt(key, value, time).apply();
        assertEquals(value, prefs.getInt(key, -1));
        threadSleep(time / 3);
        assertEquals(value, prefs.getInt(key, -1));
        threadSleep(time);
        assertEquals(-5, prefs.getInt(key, -5));
    }

    @Test
    public void longDefaultExpiration() {
        String key = "longDefaultExpiration";
        long value = -(long) (Math.random() * Long.MAX_VALUE);
        prefs.edit().putLong(key, value).apply();
        assertEquals(value, prefs.getLong(key, -1));
        threadSleep(defaultExpiresAfter / 3);
        assertEquals(value, prefs.getLong(key, -1));
        threadSleep(defaultExpiresAfter);
        assertEquals(-5, prefs.getLong(key, -5));
    }

    @Test
    public void longCustomExpiration() {
        String key = "longCustomExpiration";
        long value = (long) (Math.random() * Long.MAX_VALUE);
        int time = 150;
        prefs.edit().putLong(key, value, time).apply();
        assertEquals(value, prefs.getLong(key, -1));
        threadSleep(time / 3);
        assertEquals(value, prefs.getLong(key, -1));
        threadSleep(time);
        assertEquals(-5, prefs.getLong(key, -5));
    }

    @Test
    public void floatDefaultExpiration() {
        String key = "floatDefaultExpiration";
        float value = -(float) (Math.random() * Float.MAX_VALUE);
        prefs.edit().putFloat(key, value).apply();
        assertEquals(value, prefs.getFloat(key, -1.6f), 0.0001);
        threadSleep(defaultExpiresAfter / 3);
        assertEquals(value, prefs.getFloat(key, -1.6f), 0.0001);
        threadSleep(defaultExpiresAfter);
        assertEquals(-5.34f, prefs.getFloat(key, -5.34f), 0.0001);
    }

    @Test
    public void floatCustomExpiration() {
        String key = "floatCustomExpiration";
        float value = (float) (Math.random() * Float.MAX_VALUE);
        int time = 50;
        prefs.edit().putFloat(key, value, time).apply();
        assertEquals(value, prefs.getFloat(key, -10), 0.0001);
        threadSleep(time / 3);
        assertEquals(value, prefs.getFloat(key, -10), 0.0001);
        threadSleep(time);
        assertEquals(-5, prefs.getFloat(key, -5), 0.0001);
    }

    @Test
    public void booleanDefaultExpiration() {
        String key = "booleanDefaultExpiration";
        boolean value = (int) (Math.random() * 2) == 1;
        prefs.edit().putBoolean(key, value).apply();
        assertEquals(value, prefs.getBoolean(key, !value));
        threadSleep(defaultExpiresAfter / 3);
        assertEquals(value, prefs.getBoolean(key, !value));
        threadSleep(defaultExpiresAfter);
        assertEquals(!value, prefs.getBoolean(key, !value));
    }

    @Test
    public void booleanCustomExpiration() {
        String key = "booleanCustomExpiration";
        boolean value = (int) (Math.random() * 2) == 1;
        long time = 127;
        prefs.edit().putBoolean(key, value, time).apply();
        assertEquals(value, prefs.getBoolean(key, !value));
        threadSleep(time / 3);
        assertEquals(value, prefs.getBoolean(key, !value));
        threadSleep(time);
        assertEquals(!value, prefs.getBoolean(key, !value));
    }

    @Test
    public void remove() {
        final String key = "toBeRemoved";
        final int value = 13;
        prefs.edit().putLong(key, value).apply();
        assertEquals(value, prefs.getLong(key, 0));
        prefs.edit().remove(key).apply();
        assertEquals(0, prefs.getLong(key, 0));
    }

    public void removeUsingNull() {
        String key = "removeUsingNull";
        String value = "lskdfjds";
        String defValue = "default";
        prefs.edit().putString(key, value).apply();
        assertEquals(value, prefs.getString(key, defValue));
        prefs.edit().putString(key, null).apply();
        assertEquals(defValue, prefs.getString(key, defValue));

        prefs.edit().putStringSet(key, new HashSet<String>()).apply();
        assertNotNull(prefs.getStringSet(key, null));
        prefs.edit().putStringSet(key, null);
        assertNull(prefs.getStringSet(key, new HashSet<String>()));
    }

    @Test
    public void listeners() {
        final String key = "keyListeners";
        final String key2 = "keyListeners2";
        final SharedPreferences.OnSharedPreferenceChangeListener listener = Mockito.mock(SharedPreferences.OnSharedPreferenceChangeListener.class);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        prefs.edit().putString(key, "ldkfj").apply();
        final ArgumentCaptor<SharedPreferences> shPac = ArgumentCaptor.forClass(SharedPreferences.class);
        final ArgumentCaptor<String> keyAc = ArgumentCaptor.forClass(String.class);
        Mockito.verify(listener, timeout(1000).times(1)).onSharedPreferenceChanged(shPac.capture(), keyAc.capture());
        assertEquals(prefs, shPac.getValue());
        assertEquals(key, keyAc.getValue());
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
        prefs.edit().putString(key2, "fdslj").apply();
        prefs.edit().remove(key).remove(key2).apply();
        Mockito.verifyNoMoreInteractions(listener);
    }

    private void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}