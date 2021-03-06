package com.example.mycontactlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

public class ContactDataSource {
    private SQLiteDatabase database;
    private ContactDBHelper dbHelper;

    public ContactDataSource(Context context) {
        dbHelper = new ContactDBHelper(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }


    public void close() {
        dbHelper.close();
    }


    public boolean insertContact(Contact c) {
        boolean didSucceed = false;
        try {
            ContentValues initialValues = new ContentValues();

            initialValues.put("contactname", c.getContactName());
            initialValues.put("streetaddress", c.getStreetAddress());
            initialValues.put("city", c.getCity());
            initialValues.put("state", c.getState());
            initialValues.put("zipcode", c.getZipCode());
            initialValues.put("phonenumber", c.getPhoneNumber());
            initialValues.put("cellnumber", c.getCellNumber());
            initialValues.put("email", c.getEMail());
            initialValues.put("birthday",String.valueOf(c.getBirthday().getTimeInMillis()));

            if (c.getPicture() != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                c.getPicture().compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] photo = baos.toByteArray();
                initialValues.put("contactphoto", photo);
            } // To store a bitmap to the database, it must first be converted to a byte array.
            // This code uses standard objects in the Android SDK to do this conversion.
            // After photo is converted, it is placed into the values to be updated.

            didSucceed = database.insert("contact", null, initialValues) > 0;
        }
        catch (Exception e) {
            //Do nothing -will return false if there is an exception
        }
        return didSucceed;
    }


    public boolean updateContact(Contact c) {
        boolean didSucceed = false;
        try {
            Long rowId = (long) c.getContactID();
            ContentValues updateValues = new ContentValues();

            updateValues.put("contactname", c.getContactName());
            updateValues.put("streetaddress", c.getStreetAddress());
            updateValues.put("city", c.getCity());
            updateValues.put("state", c.getState());
            updateValues.put("zipcode", c.getZipCode());
            updateValues.put("phonenumber", c.getPhoneNumber());
            updateValues.put("cellnumber", c.getCellNumber());
            updateValues.put("email", c.getEMail());
            updateValues.put("birthday", String.valueOf(c.getBirthday().getTimeInMillis()));

            if (c.getPicture() != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                c.getPicture().compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] photo = baos.toByteArray();
                updateValues.put("contactphoto", photo);
            }

            didSucceed = database.update("contact", updateValues, "_id=" + rowId, null) > 0;
        }
        catch (Exception e) {
            //Do nothing -will return false if there is an exception
        }
        return didSucceed;
    }


    public int getLastContactId() {
        int lastId;
        try {
            String query = "Select MAX(_id) from contact";
            Cursor cursor = database.rawQuery(query, null);

            cursor.moveToFirst();
            lastId = cursor.getInt(0);
            cursor.close();
        }
        catch (Exception e) {
            lastId = -1;
        }
        return lastId;
    }

    //method to get contact name data from db
    public ArrayList<String> getContactName() {
        ArrayList<String> contactNames = new ArrayList<>(); //return value is arraylist of strings
        try {
            String query = "Select contactname from contact"; //query to retrieve contact names from db
            Cursor cursor = database.rawQuery(query, null); //holds query results

            cursor.moveToFirst(); //initialize loop by moving cursor to first record
            while (!cursor.isAfterLast()) { //while loop tests if the end of cursor's record set is reached
                contactNames.add(cursor.getString(0)); //contact name added to arraylist each iteration
                cursor.moveToNext(); //cursor moves to next record - causes infinite loop without this
            }
            cursor.close();
        }
        catch (Exception e) {
            contactNames = new ArrayList<String>(); //in case of crash, calling activity tests for empty list
        }
        return contactNames;
    }

    //method to retrieve contact data for all contacts from db
    public ArrayList<Contact> getContacts(String sortField, String sortOrder) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        try {
            String query = "SELECT  * FROM contact ORDER BY " + sortField + " " + sortOrder;
            Cursor cursor = database.rawQuery(query, null);

            Contact newContact;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) { //new contact obj instance for each cursor record
                newContact = new Contact(); //values get added to attribute in new obj
                newContact.setContactID(cursor.getInt(0)); //index starts at 0 for cursor
                newContact.setContactName(cursor.getString(1));
                newContact.setStreetAddress(cursor.getString(2));
                newContact.setCity(cursor.getString(3));
                newContact.setState(cursor.getString(4));
                newContact.setZipCode(cursor.getString(5));
                newContact.setPhoneNumber(cursor.getString(6));
                newContact.setCellNumber(cursor.getString(7));
                newContact.setEMail(cursor.getString(8));
                Calendar calendar = Calendar.getInstance(); //new calendar obj to hold contact's bday
                calendar.setTimeInMillis(Long.valueOf(cursor.getString(9)));
                newContact.setBirthday(calendar); //insert new bday obj into contact object
                contacts.add(newContact);
                cursor.moveToNext();
            }
            cursor.close();
        }
        catch (Exception e) {
            contacts = new ArrayList<Contact>();
        }
        return contacts;
    }

    //method similar to getContacts but to return single contact rather than arrayList
    public Contact getSpecificContact(int contactId) { //param holds id of contact
        Contact contact = new Contact(); //returns contact obj instead of arrayList
        String query = "SELECT  * FROM contact WHERE _id =" + contactId; //where clause specifies id value to return
        Cursor cursor = database.rawQuery(query, null);

        //no while loop needed bc only one contact is returned
        //cursor moves to first record - if contact found, contact obj is populated
        if (cursor.moveToFirst()) {
            contact.setContactID(cursor.getInt(0));
            contact.setContactName(cursor.getString(1));
            contact.setStreetAddress(cursor.getString(2));
            contact.setCity(cursor.getString(3));
            contact.setState(cursor.getString(4));
            contact.setZipCode(cursor.getString(5));
            contact.setPhoneNumber(cursor.getString(6));
            contact.setCellNumber(cursor.getString(7));
            contact.setEMail(cursor.getString(8));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.valueOf(cursor.getString(9)));
            contact.setBirthday(calendar);

            // load the Contact object with the picture. This does not need to be done in the
            // getContacts method because the picture is not used by any activity that uses the
            // whole set of contacts. Returning the picture from the database is essentially
            // the reverse process from saving to the database
            byte[] photo = cursor.getBlob(10);
            // The byte array is retrieved from the database and is then tested to determine if
            // a picture has been stored. The 10 in the getBlob method is the index
            // of the contactphoto field in the contact table
            if (photo != null) {
                ByteArrayInputStream imageStream = new ByteArrayInputStream(photo);
                Bitmap thePicture = BitmapFactory.decodeStream(imageStream);
                contact.setPicture(thePicture);
                // The conversion from byte array to Bitmap will cause a crash if no picture
                // is stored. After the data has been converted, it is set in the Contact object.
            }

            // if no contact retrieved, moveToFirst is false and contact will not populate
            cursor.close();
        }
        return contact;
    }

    //method passed to the id number of the contact to delete
    public boolean deleteContact(int contactId) {
        boolean didDelete = false; //success/fail return value
        try { //call db delete method
            didDelete = database.delete("contact", "_id=" + contactId, null) > 0;
        } //(name of table to delete from, WHERE determines which records to delete, string array of criteria for deletion)
        catch (Exception e) {
            //Do nothing -return value already set to false
        }
        return didDelete;
    }

}
