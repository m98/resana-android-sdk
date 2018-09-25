package io.resana.player.FileManager;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

/**
 * Created by hojjatimani on 5/22/2016 AD.
 */
public class Media extends Entity {

    transient Bitmap thumbnail;

    public Media(Dir parent, String name) {
        super(parent, name, Type.Media);
    }

    public Bitmap getThumbnail() {
        if (thumbnail == null)
            createThumbnail();
        return thumbnail;
    }

    private void createThumbnail() {
        thumbnail = ThumbnailUtils.createVideoThumbnail(getPath(), MediaStore.Video.Thumbnails.MINI_KIND);

    }
}