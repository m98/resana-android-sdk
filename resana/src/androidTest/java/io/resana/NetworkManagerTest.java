package io.resana;


import android.util.Log;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NetworkManagerTest {
    private static final String TAG = "NetworkManager";

    @Test
    public void openConnectionTest() throws IOException {
        String url = "http://tpsl.ir/TZBXR";
        int responseCode = 0;
        try {
            responseCode = NetworkManager.openConnection(url).getResponseCode();
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
        assertEquals(200, responseCode);
    }
}
