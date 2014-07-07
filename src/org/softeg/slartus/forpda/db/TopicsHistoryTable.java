package org.softeg.slartus.forpda.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.softeg.slartus.forpda.MyApp;
import org.softeg.slartus.forpda.classes.ExtTopic;
import org.softeg.slartus.forpda.classes.Forum;
import org.softeg.slartus.forpda.classes.Themes;
import org.softeg.slartus.forpda.common.Log;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: slinkin
 * Date: 22.11.12
 * Time: 7:27
 * To change this template use File | Settings | File Templates.
 */
public class TopicsHistoryTable {

    public static final String TABLE_NAME = "TopicsHistory";
    public static final String COLUMN_TOPIC_ID = "Topic_id";
    public static final String COLUMN_DATETIME = "DateTime";
    public static final String COLUMN_URL = "Url";

    public static void getTopicsHistory(Themes res) throws IOException {

        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            DbHelper dbHelper = new DbHelper(MyApp.getInstance());
            db = dbHelper.getWritableDatabase();

            String selection = null;
            String[] selectionArgs = null;
//            if (res.size() > 0) {
//                selection = COLUMN_DATETIME + ">?";
//                selectionArgs = new String[]{DbHelper.DateTimeFormat.format(res.get(res.size() - 1).getLastMessageDate()) + ""};
//            }

            res.setThemesCountInt(BaseTable.getRowsCount(db, "TopicsHistoryView"));

            c = db.query("TopicsHistoryView", null, selection, selectionArgs, null, null, null, res.size() + ", 20");
            Forum forum = ForumsTable.loadForumsTree();
            if (c.moveToFirst()) {
                int columnIdIndex = c.getColumnIndex(TopicsTable.COLUMN_ID);
                int columnTitleIndex = c.getColumnIndex(TopicsTable.COLUMN_TITLE);
                int columnDescriptionIndex = c.getColumnIndex(TopicsTable.COLUMN_DESCRIPTION);
                int columnForumIdIndex = c.getColumnIndex(TopicsTable.COLUMN_FORUM_ID);
                int columnDateTimeIndex = c.getColumnIndex(COLUMN_DATETIME);
                int columnUrlIndex = c.getColumnIndex(COLUMN_URL);
                //  int columnForumTitleIndex = c.getColumnIndex("ForumTitle");
                do {
                    String id = c.getString(columnIdIndex);
                    String title = c.getString(columnTitleIndex);
                    String description = c.getString(columnDescriptionIndex);
                    String forumId = c.getString(columnForumIdIndex);
                    String forumTitle = null;
                    Forum f = forum.findById(forumId, true, false);
                    if (f != null)
                        forumTitle = f.getTitle();
                    Date dateTime = null;
                    try {
                        dateTime = DbHelper.parseDate(c.getString(columnDateTimeIndex));
                    } catch (ParseException e) {
                        Log.e(MyApp.getContext(), e);
                    }
                    //!TODO:
                    String url = c.getString(columnUrlIndex);

                    ExtTopic topic = new ExtTopic(id, title);
                    topic.setDescription(description);
                    topic.setForumId(forumId);
                    topic.setForumTitle(forumTitle);
                    topic.setLastMessageDate(dateTime);
                    res.add(topic);
                } while (c.moveToNext());

            }

        } finally {
            if (db != null) {
                if (c != null)
                    c.close();
                db.close();
            }
        }

    }

    public static void addHistory(ExtTopic topic, String url) {
        if (topic.getId() == null) return;
        TopicsTable.addTopic(topic, true);
        SQLiteDatabase db = null;
        Cursor c = null;
        try {

            DbHelper dbHelper = new DbHelper(MyApp.getInstance());
            db = dbHelper.getWritableDatabase();

            db.execSQL("delete from " + TABLE_NAME + " where " + COLUMN_TOPIC_ID + "=" + topic.getId());

            ContentValues values = new ContentValues();
            values.put(COLUMN_TOPIC_ID, topic.getId());
            values.put(COLUMN_DATETIME, DbHelper.DateTimeFormat.format(new Date()));
            values.put(COLUMN_URL, url);

            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (Exception ex) {
            Log.e(MyApp.getInstance(), ex);
        } finally {
            if (db != null) {
                if (c != null)
                    c.close();
                db.close();
            }
        }
    }
}
