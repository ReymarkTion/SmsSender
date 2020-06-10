package com.witechsms.smssender;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String LOGCAT = "sms-sender-queue";

    public DatabaseHandler(Context applicationcontext) {
        super(applicationcontext, "WitechSmsSender", null, 1);
        Log.d(LOGCAT, "Created");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String query;

        // StudentAttendanceLog table
        /*query = "CREATE TABLE StudentAttendanceLog ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id TEXT, " +
                "date_time TEXT," +
                "synced INTEGER DEFAULT 0)";
        database.execSQL(query);
        Log.d(LOGCAT, "StudentAttendanceLog Created");
        Log.d(LOGCAT, "query: " + query); */


        // SmsState table
        query = "CREATE TABLE SmsState ( " +
                "time_stamp TEXT PRIMARY KEY, " +
                "parent_mnumber TEXT," +
                "sms_body TEXT," +
                "sms_state INTEGER DEFAULT 0)";
        database.execSQL(query);
        Log.d(LOGCAT, "SmsState Created");
        Log.d(LOGCAT, "query: " + query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
        String query;

        /*query = "DROP TABLE IF EXISTS SchoolSettings";
        database.execSQL(query); */

        query = "DROP TABLE IF EXISTS SmsState";
        database.execSQL(query);

        onCreate(database);
    }


    /**
     *
     * Sms to send new table
     *
     */

    public void deleteSms(String time_stamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM SmsState WHERE time_stamp = '" + time_stamp + "'");
        db.close();
    }

    public HashMap<String, String> getAnSms() {

        HashMap<String, String> hm = new HashMap<>();
        String selectQuery = "SELECT * FROM SmsState WHERE sms_state = 0 ORDER BY time_stamp ASC LIMIT 1";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.getCount() == 0)
            return null;

        if (cursor.moveToFirst()) {
            do {
                hm.put("time_stamp", cursor.getString(0));
                hm.put("number", cursor.getString(1));
                hm.put("sms", cursor.getString(2));

                Log.d("smsdebug", "=====================");
                Log.d("smsdebug", "time_stamp: " + cursor.getString(0));
                Log.d("smsdebug", "number: " + cursor.getString(1));
                Log.d("smsdebug", "sms: " + cursor.getString(2));
            } while (cursor.moveToNext());
        }
        //hm.put("status", "ok");
        Log.d("smsdebug", "^------------------^");
        return hm;
    }

    public void updateSmsState(String time_stamp, int state) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put("sms_state", state);
        database.update("SmsState", value, "time_stamp = '" + time_stamp + "'", null);
    }

    public void updateSmsTS(String n_ts, String o_ts) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put("time_stamp", n_ts);
        database.update("SmsState", value, "time_stamp = '" + o_ts + "'", null);
    }

    public int getLogSize() {
        String selectQuery = "SELECT * FROM SmsState";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        return cursor.getCount();
    }

    public void insertSms(String time_stamp, String number, String sms) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("time_stamp", time_stamp);
        values.put("parent_mnumber", number);
        values.put("sms_body", sms);

        database.insert("SmsState", null, values);
        database.close();
    }

    public void getAllSms() {
        String selectQuery = "SELECT * FROM SmsState";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Log.d("smsdebug", "=====================");
                Log.d("smsdebug", "time_stamp: " + cursor.getString(0));
                Log.d("smsdebug", "number: " + cursor.getString(1));
                Log.d("smsdebug", "sms: " + cursor.getString(2));
                Log.d("smsdebug", "state: " + cursor.getString(3));
            } while (cursor.moveToNext());
        }
        Log.d("smsdebug", "^------------------^");
    }

}