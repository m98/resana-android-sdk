package io.resana;

import android.content.Context;
import android.os.Parcel;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author Hojjat Imani
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DtosTest {
    String li = "{\"title\":\"2KfZvtmE24zaqduM2LTZhiDYr9in2YbYsdmI\",\"text\":\"2K/ZiNiz2Kog2b7bjNiv2Kcg2qnZhtiMINmG2LjYsSDYqNiv2YfYjCDYs9qp2Ycg2KjYqNixIQ==\",\"image\":\"https://static.resana.io/visuals/2379323?2\",\"index\":-3,\"ts\":\"1502520496\",\"order\":10485,\"type\":5,\"cat\":\"m10023\",\"ttl\":600,\"ctl\":2,\"mv\":4,\"pic\":\"https://static.resana.io/visuals/2379323??12\",\"v\":[{\"id\":2379323,\"url\":\"https://static.resana.io/visuals/2379323??12?12\",\"type\":\"image/png\",\"w\":200,\"h\":200}],\"ver\":1,\"rl\":{\"l\":\"http://tpsl.ir/pEZze\",\"t\":\"Li4u\"},\"apk\":{\"url\":\"https://static.resana.io/apk/10345.apk?12\",\"pkg\":\"ir.peykebartar.android\",\"net\":1,\"v\":1},\"report\":{\"ttl\":86400,\"t\":0,\"param\":\"ir.peykebartar.android\"},\"lnd\":{\"url\":\"https://static.resana.io/landing/10345?12\",\"t\":0},\"sc\":[{\"u\":\"url\", \"w\":3, \"m\":\"POST\", \"h\":{\"key\":\"value\", \"k2\":\"v2\"}} , {\"u\":\"url2\" , \"w\":1, \"m\":\"GET\", \"h\":{}, \"p\":{\"key2\": \"حجت\"}}]}";

    @Test
    public void listItemCreation() {
        final ListItemDto item = DtoParser.parse(li, ListItemDto.class);

        assertNotNull(item);
        assertEquals(item.title, "اپلیکیشن دانرو");
        assertEquals(item.text, "دوست پیدا کن، نظر بده، سکه ببر!");
        assertEquals(item.image, "https://static.resana.io/visuals/2379323?2");
        assertEquals(item.ts, "1502520496");
        assertEquals(item.order, Long.valueOf(10485));
        assertEquals(item.type, Integer.valueOf(AdDto.AD_TYPE_LIST_ITEM));
        assertEquals(item.cat, "m10023");
        assertEquals(item.ttl, 600);
        assertEquals(item.ctl, 2);
        assertEquals(item.maxView, 4);
        assertEquals(item.index, -3);
        assertEquals(item.version, 1);
        assertEquals(item.resanaLabel.label, "http://tpsl.ir/pEZze");
        assertEquals(item.resanaLabel.text, "...");
        assertEquals(item.apk.url, "https://static.resana.io/apk/10345.apk?12");
        assertEquals(item.apk.pkg, "ir.peykebartar.android");
        assertEquals(item.apk.version, 1);
        assertEquals(item.apk.net, Integer.valueOf(1));
        assertEquals(item.apk.prepareTime, 0);
        assertEquals(item.report.type, Integer.valueOf(0));
        assertEquals(item.report.param, "ir.peykebartar.android");
        assertEquals(item.report.ttl, 86400);
        assertEquals(item.landing.url, "https://static.resana.io/landing/10345?12");
        assertEquals(item.landing.type, Integer.valueOf(0));
        assertEquals(item.simulateClicks.length, 2);
        assertEquals(item.simulateClicks[0].url, "url");
        assertEquals(item.simulateClicks[0].when, Integer.valueOf(3));
        assertEquals(item.simulateClicks[0].method, "POST");
        assertEquals(item.simulateClicks[0].headers.size(), 2);
        assertEquals(item.simulateClicks[0].headers.get("k2"), "v2");
        assertNull(item.simulateClicks[0].params);
        assertEquals(item.simulateClicks[1].url, "url2");
        assertEquals(item.simulateClicks[1].when, Integer.valueOf(1));
        assertEquals(item.simulateClicks[1].method, "GET");
        assertNotNull(item.simulateClicks[1].headers);
        assertEquals(item.simulateClicks[1].headers.size(), 0);
        assertEquals(item.simulateClicks[1].params.size(), 1);
        assertEquals(item.simulateClicks[1].params.get("key2"), "حجت");
    }

    @Test
    public void listItemParcalable() {
        final ListItemDto ad = DtoParser.parse(li, ListItemDto.class);
        final Parcel parcel = Parcel.obtain();
        ad.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final ListItemDto item = ListItemDto.CREATOR.createFromParcel(parcel);

        assertNotNull(item);
        assertEquals(item.title, "اپلیکیشن دانرو");
        assertEquals(item.text, "دوست پیدا کن، نظر بده، سکه ببر!");
        assertEquals(item.image, "https://static.resana.io/visuals/2379323?2");
        assertEquals(item.ts, "1502520496");
        assertEquals(item.order, Long.valueOf(10485));
        assertEquals(item.type, Integer.valueOf(5));
        assertEquals(item.cat, "m10023");
        assertEquals(item.ttl, 600);
        assertEquals(item.ctl, 2);
        assertEquals(item.maxView, 4);
        assertEquals(item.index, -3);
        assertEquals(item.version, 1);
        assertEquals(item.resanaLabel.label, "http://tpsl.ir/pEZze");
        assertEquals(item.resanaLabel.text, "...");
        assertEquals(item.apk.url, "https://static.resana.io/apk/10345.apk?12");
        assertEquals(item.apk.pkg, "ir.peykebartar.android");
        assertEquals(item.apk.version, 1);
        assertEquals(item.apk.net, Integer.valueOf(1));
        assertEquals(item.apk.prepareTime, 0);
        assertEquals(item.report.type, Integer.valueOf(0));
        assertEquals(item.report.param, "ir.peykebartar.android");
        assertEquals(item.report.ttl, 86400);
        assertEquals(item.landing.url, "https://static.resana.io/landing/10345?12");
        assertEquals(item.landing.type, Integer.valueOf(0));
        assertEquals(item.simulateClicks.length, 2);
        assertEquals(item.simulateClicks[0].url, "url");
        assertEquals(item.simulateClicks[0].when, Integer.valueOf(3));
        assertEquals(item.simulateClicks[0].method, "POST");
        assertEquals(item.simulateClicks[0].headers.size(), 2);
        assertEquals(item.simulateClicks[0].headers.get("k2"), "v2");
        assertNull(item.simulateClicks[0].params);
        assertEquals(item.simulateClicks[1].url, "url2");
        assertEquals(item.simulateClicks[1].when, Integer.valueOf(1));
        assertEquals(item.simulateClicks[1].method, "GET");
        assertNotNull(item.simulateClicks[1].headers);
        assertEquals(item.simulateClicks[1].headers.size(), 0);
        assertEquals(item.simulateClicks[1].params.size(), 1);
        assertEquals(item.simulateClicks[1].params.get("key2"), "حجت");
    }

    @Test
    public void listItemSerializable() {
        final ListItemDto ad = DtoParser.parse(li, ListItemDto.class);
        Context context = InstrumentationRegistry.getContext();
        ListItemDto item = null;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(context.getCacheDir(), "testTemp")));
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(context.getCacheDir(), "testTemp")));
            oos.writeObject(ad);
            oos.flush();
            oos.close();
            item = (ListItemDto) ois.readObject();
        } catch (Exception ignored) {
            fail("could not write serializable to file");
        }
        assertNotNull(item);
        assertEquals(item.title, "اپلیکیشن دانرو");
        assertEquals(item.text, "دوست پیدا کن، نظر بده، سکه ببر!");
        assertEquals(item.image, "https://static.resana.io/visuals/2379323?2");
        assertEquals(item.ts, "1502520496");
        assertEquals(item.order, Long.valueOf(10485));
        assertEquals(item.type, Integer.valueOf(5));
        assertEquals(item.cat, "m10023");
        assertEquals(item.ttl, 600);
        assertEquals(item.ctl, 2);
        assertEquals(item.maxView, 4);
        assertEquals(item.index, -3);
        assertEquals(item.version, 1);
        assertEquals(item.resanaLabel.label, "http://tpsl.ir/pEZze");
        assertEquals(item.resanaLabel.text, "...");
        assertEquals(item.apk.url, "https://static.resana.io/apk/10345.apk?12");
        assertEquals(item.apk.pkg, "ir.peykebartar.android");
        assertEquals(item.apk.version, 1);
        assertEquals(item.apk.net, Integer.valueOf(1));
        assertEquals(item.apk.prepareTime, 0);
        assertEquals(item.report.type, Integer.valueOf(0));
        assertEquals(item.report.param, "ir.peykebartar.android");
        assertEquals(item.report.ttl, 86400);
        assertEquals(item.landing.url, "https://static.resana.io/landing/10345?12");
        assertEquals(item.landing.type, Integer.valueOf(0));
        assertEquals(item.simulateClicks.length, 2);
        assertEquals(item.simulateClicks[0].url, "url");
        assertEquals(item.simulateClicks[0].when, Integer.valueOf(3));
        assertEquals(item.simulateClicks[0].method, "POST");
        assertEquals(item.simulateClicks[0].headers.size(), 2);
        assertEquals(item.simulateClicks[0].headers.get("k2"), "v2");
        assertNull(item.simulateClicks[0].params);
        assertEquals(item.simulateClicks[1].url, "url2");
        assertEquals(item.simulateClicks[1].when, Integer.valueOf(1));
        assertEquals(item.simulateClicks[1].method, "GET");
        assertNotNull(item.simulateClicks[1].headers);
        assertEquals(item.simulateClicks[1].headers.size(), 0);
        assertEquals(item.simulateClicks[1].params.size(), 1);
        assertEquals(item.simulateClicks[1].params.get("key2"), "حجت");
    }
}
