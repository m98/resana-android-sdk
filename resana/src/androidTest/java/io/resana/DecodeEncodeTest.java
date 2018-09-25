package io.resana;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DecodeEncodeTest {
    private static final String TAG = "DecodeEncodeTest";

    @Test
    public void test() {
        final List<String> pkgs = ApkManager.getInstance(InstrumentationRegistry.getContext()).getInstalledPackagesFromSystem();
        Log.d(TAG, "pkgs.len=" + pkgs.size());
        String s = Arrays.toString(pkgs.toArray());
        Log.d(TAG, "len=" + s.length() + "  raw:" + s);
        final String encoded = DataCollector.encode(s);
        Log.d(TAG, "len=" + encoded.length() + "  encoded:" + encoded);
        final String decoded = decode(encoded);
        Log.d(TAG, "len=" + decoded.length() + "  decoded:" + decoded);
        assertEquals(s, decoded);
    }

    static String decode(String val) {
        Character c = null;
        if (val.length() % 2 == 1) {
            c = val.charAt(0);
            val = val.substring(1);
        }
        String part1 = "";
        String part2 = "";
        for (int i = 0; i < val.length(); i = i + 2) {
            part1 += val.charAt(i + 1);
            part2 += val.charAt(i);
        }
        part1 = new StringBuilder(part1).reverse().toString();
        val = part1 + (c != null ? c : "") + part2;
        val = new StringBuilder(val).reverse().toString();
        return BefrestImpl.Util.decodeBase64(val);
    }
}
