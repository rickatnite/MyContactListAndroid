package com.example.mycontactlist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ContactDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mycontacts.db";
    private static final int DATABASE_VERSION = 2;
    //update version anytime the database is changed

    // Database creation sql statement
    private static final String CREATE_TABLE_CONTACT =
         "create table contact (_id integer primary key autoincrement, "
                 + "contactname text not null, streetaddress text, "
                 + "city text, state text, zipcode text, "
                 + "phonenumber text, cellnumber text, "
                 + "email text, birthday text, contactphoto blob);";

    // add blob for contact photo above - The blob data type can hold any type of binary data
    // and is typically used for picture, audio, and video objects

    public ContactDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CONTACT);
    }


    // this method was previously written to delete the current table and create a new one when
    // the database is updated. However, using this approach, all the user’s contacts will be
    // deleted! in the dev stage, this is OK, but this is not an option for a released app.
    // below changes allow a change to the database structure without losing all the user data
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        Log.w(ContactDBHelper.class.getName(),
//                "Upgrading database from version " + oldVersion + " to "
//                        + newVersion + ", which will destroy all old data");
//        db.execSQL("DROP TABLE IF EXISTS contact");
//        onCreate(db);
        try {
            db.execSQL("ALTER TABLE contact ADD COLUMN contactphoto blob");
        }
        catch (Exception e) {
            //do nothing
        } // use try/catch so that if the field has already been added, it won’t crash the app
    }

}
