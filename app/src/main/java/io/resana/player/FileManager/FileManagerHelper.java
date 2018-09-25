package io.resana.player.FileManager;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by hojjatimani on 5/23/2016 AD.
 */
public class FileManagerHelper {
    public static final String FILE_NAME = "fileSystemFileName";

    public static boolean onlyMediaContainingSubDirs = true;
    public static boolean collapseUnnecessaryIntermediateSubDirs = true;
    public static int viewType;

    public static Dir getRawFileSystem(Context context) {
        Dir dir = null;
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            ObjectInputStream is = new ObjectInputStream(fis);
            dir = (Dir) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dir;
    }

    public static void saveFileSystem(Context context, Dir fileSystem) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(fileSystem);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
