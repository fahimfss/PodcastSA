package com.example.fahim.podcastsa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

/**
 * Created by fahim on 04-Jul-17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME="PodcastsDB";
    private static final String TABLE_NAME="Podcasts";
    private static final String COLUMN_ID="Id";
    private static final String COLUMN_LINK="Link";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_LINK + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void putLink(String link, DatabaseHelper helper){
        SQLiteDatabase db = helper.getWritableDatabase();
        if(isLinkExist(link, helper)) return;

        ContentValues values = new ContentValues();
        values.put(COLUMN_LINK, link);

        db.insert(TABLE_NAME, null, values);
    }

    public boolean isLinkExist(String link, DatabaseHelper helper){
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = db.query(
                TABLE_NAME,                     // The table to query
                new String[]{COLUMN_LINK},      // The columns to return
                COLUMN_LINK + " = ?",           // The columns for the WHERE clause
                new String[]{link},                           // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );
        if(cursor!=null && cursor.moveToFirst()){
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }
}
