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

import android.content.Context;

import java.util.Iterator;

import static io.resana.ResanaPreferences.PREF_LAST_RECEIVED_MESSAGES;
import static io.resana.ResanaPreferences.getPrefs;
import static io.resana.ResanaPreferences.saveString;
class MessageIdPersister extends BoundedLinkedHashSet<String> {
    private static final String TAG = ResanaLog.TAG_PREF + "MessageIdPersist";
    Context appContext;

    public MessageIdPersister(Context c) {
        super(100);
        appContext = c.getApplicationContext();
        load(appContext);
    }

    private void load(Context c) {
        String s = getPrefs(c).getString(PREF_LAST_RECEIVED_MESSAGES, "");
        if (s.length() > 0)
            for (String s1 : s.split(","))
                add(s1);
    }

    public void save() {
        long start = System.currentTimeMillis();
        saveString(appContext, PREF_LAST_RECEIVED_MESSAGES, toString());
        ResanaLog.v(TAG, "saved lastReceivedMessageIds");
        ResanaLog.v(TAG, "save duration:" +  (System.currentTimeMillis() - start));
    }

    @Override
    public String toString() {
        Iterator<String> it = iterator();
        if (!it.hasNext())
            return "";
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            sb.append(it.next());
            if (!it.hasNext())
                return sb.toString();
            sb.append(',');
        }
    }
}
