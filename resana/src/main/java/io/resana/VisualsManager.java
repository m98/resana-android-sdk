package io.resana;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.resana.ResanaPreferences.PREF_VISUAL_INDEX;
import static io.resana.ResanaPreferences.getString;
import static io.resana.ResanaPreferences.saveString;

class VisualsManager {
    private static final String TAG = "VisualsManager";

    /**
     * This method will save visual indexes of an ad.
     * after we can get an index of a visual randomly
     * @param context
     * @param ad
     */
    static void saveVisualsIndex(Context context, Ad ad) {
        if (ad.getType() != AdDto.AD_TYPE_NATIVE)
            return;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < ((NativeDto) ad.data).visuals.size(); i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes);
        String indexString = "";
        for (int i = 0; i < indexes.size(); i++) {
            indexString += indexes.get(i) + ",";
        }
        saveString(context, ad.getId() + PREF_VISUAL_INDEX, indexString);
    }

    /**
     * This method will return list of visual indexes of an ad which is shuffled.
     * @param context
     * @param ad
     * @return
     */
    static List<Integer> getDownloadingVisualsIndex(Context context, Ad ad) {
        List<Integer> indexes = new ArrayList<>();
        String[] indexesString = getString(context, ad.getId() + PREF_VISUAL_INDEX, "").split(",");
        for (int i = 0; i < indexesString.length; i++) {
            indexes.add(Integer.parseInt(indexesString[i]));
        }
        return indexes;
    }

    /**
     * This method will randomly return an index of a visual.
     * @param context
     * @param ad
     * @return
     */
    static int getVisualIndex(Context context, NativeAd ad) {
        String indexString = getString(context, ad.getOrder() + PREF_VISUAL_INDEX, "");
        String indexString2 = indexString.split(",")[0];
        int index = 0;
        if (!indexString2.equals(""))
            index = Integer.parseInt(indexString.split(",")[0]);
        indexString = indexString.substring(indexString.indexOf(",") + 1);
        saveString(context, ad.getOrder() + PREF_VISUAL_INDEX, indexString);
        return index;
    }
}
