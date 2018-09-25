package io.resana;


import android.util.Log;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

public class NetworkHelperTest {
    private static final String TAG = "NetworkHelper";

    @Test
    public void openConnectionTest() throws IOException {
        String url = "http://tpsl.ir/TZBXR";
        int responseCode = 0;
        try {
            responseCode = NetworkHelper.openConnection(url).getResponseCode();
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
        assertEquals(200, responseCode);
    }
}
