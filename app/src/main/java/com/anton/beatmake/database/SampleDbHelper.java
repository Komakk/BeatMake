package com.anton.beatmake.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anton.beatmake.database.SampleDbSchema.SampleTable;

public class SampleDbHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "sampleBase.db";

    public SampleDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + SampleTable.NAME + "(" + " _id integer primary key autoincrement, " +
        SampleTable.Cols.TITLE + " text unique not null, " +
        SampleTable.Cols.SEQUENCE + ", " +
        SampleTable.Cols.TEMPO + ", " +
        SampleTable.Cols.CHANNEL_VOLUMES + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
