package org.softeg.slartus.forpda.db;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.softeg.slartus.forpda.MyApp;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: slinkin
 * Date: 25.03.13
 * Time: 9:27
 * To change this template use File | Settings | File Templates.
 */
public class ForumStructDbHelper extends SQLiteAssetHelper {

    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "forum_struct";

    public ForumStructDbHelper(Context context) throws IOException {
        super(context, DATABASE_NAME, MyApp.getInstance().getAppExternalFolderPath(), null, DATABASE_VERSION);
        setForcedUpgrade(10);
    }


}

