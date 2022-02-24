package com.example.mycontactlist;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.SaveDateListener {

    private Contact currentContact;
    // constants used below allow the method to respond to the permission that was asked for
    final int PERMISSION_REQUEST_PHONE = 102;
    final int PERMISSION_REQUEST_CAMERA = 103;
    final int PERMISSION_REQUEST_SMS = 104;
    // this variable is an integer that is used to identify the response from the camera
    // when it finishes. The value should be a large integer far from other built-in responses.
    final int CAMERA_REQUEST = 1888;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initListButton();
        initMapButton();
        initSettingsButton();
        initToggleButton();
        initChangeDateButton();
        initTextChangedEvents();
        initSaveButton();
        //initCallFunction();
        initImageButton();
        initMessagingFunction();

        //get passed id and call initContact to retrieve and display contact - corresponds to ContactListActivity onclick
        //checks intent for extras, if found gets contactId, retrieves contact from db, displays data in layout
        //if no extra, assigns new contact obj to currentContact making id -1
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            initContact(extras.getInt("contactId"));
        }
        else {
            currentContact = new Contact();
        }
        setForEditing(false);
    }


    // add a listener to the phone number EditTexts for the press-and-hold user action
    // this method will call a method to check the permission
    private void initCallFunction() {
        EditText editPhone = (EditText) findViewById(R.id.editHome);
        editPhone.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                checkPhonePermission(currentContact.getPhoneNumber());
                return false;
            }
        });

        EditText editCell = (EditText) findViewById(R.id.editCell);
        editCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                checkPhonePermission(currentContact.getCellNumber());
                return false;
            }
        });
    }


    // method to check if the app has permission to use the phone
    private void checkPhonePermission(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.CALL_PHONE)) {

                    Snackbar.make(findViewById(R.id.activity_main),
                            "MyContactList requires this permission to place a call from the app.",
                            Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {

                                @Override
                                public void onClick (View view) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[] {Manifest.permission.CALL_PHONE },
                                            PERMISSION_REQUEST_PHONE);
                                }
                            }).show();

                    // we need the ability to identify which permission is being requested.
                    // The method onRequestPermissionsResult is executed anytime the app asks
                    // for a permission from a user.

                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new
                                    String[]{Manifest.permission.CALL_PHONE},
                                    PERMISSION_REQUEST_PHONE);
                        }
                    } else {
                        callContact(phoneNumber);
                    }
                } else {
                    callContact(phoneNumber);
                }
    }


    // check for a different permission in the switch case statement
    // instead of doing something, we provide a message that the functionality is now available
    //@Override
    public void onRequestPermissionResult(int requestCode, @NonNull String permissions[],
                                          @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_PHONE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "You may now call from this app.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "You will not be able to make calls from this app", Toast.LENGTH_LONG).show();
                }
            }

            case PERMISSION_REQUEST_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "You may now sens SMS from this app.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "You will not be able to send SMS from this app", Toast.LENGTH_LONG).show();
                }
            }

            case PERMISSION_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Toast.makeText(MainActivity.this, "You will not be able to save" +
                            "contact pictures from this app.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    public void takePhoto() {
        // new intent is instantiated with a parameter that tells the system to open the camera
        // in image capture mode. You do not have to check whether the camera is present.
        // The manifest permission would not let the app run on the device if it has no camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // you want the activity to return a value to the app after it has completed, so you use
        // the startActivityForResult method. The parameters are the new Intent and a static
        // variable called CAMERA_REQUEST, an integer that is used to identify
        // the response from the camera when it finishes
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }


    // receives a request code that was sent to the camera, a result code, and an intent that
    // includes the data (the picture in this case) from the intent you started.
    // This method is executed when the camera finishes
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) { // is returned request code the one sent to the camera?
            if (resultCode == RESULT_OK) { // Check if the camera returned with a picture.
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                // The data from the Intent is assigned to a variable declared as a Bitmap.
                // The method .get("data") doesn’t specify a type of data to get from the extras,
                // so it must be cast to Bitmap. After the photo is captured, it is displayed in the
                // ImageButton, and the contact object’s picture attribute is set to hold the photo.
                Bitmap scaledPhoto = Bitmap.createScaledBitmap(photo, 144, 144, true);
                // The picture is scaled so that a consistent-size photo is displayed in the
                // ImageButton. The parameters of this method are the picture to be scaled, the
                // height and width to scale to in pixels, and whether a filter should be applied
                // during the scaling operation. Generally, when scaling down, this filter has no
                // effect but can change the result when scaling up.
                ImageButton imageContact = (ImageButton) findViewById(R.id.imageContact);
                imageContact.setImageBitmap(scaledPhoto);
                currentContact.setPicture(scaledPhoto);
                // add Bitmap variable and getters/setters to Contact object (Contact.java class)
            }
        }
    }


    // the method that actually dials the phone number and check if permission has been granted
    private void callContact(String phoneNumber) {
        // tells Android to use phone to make a call
        Intent intent = new Intent(Intent.ACTION_CALL);
        // phone number to be called is passed to the intent as a uniform resource identifier (URI).
        // A URI is similar to a uniform resource locator (URL), except that a URL identifies
        // a location on the web, whereas a URI can be used to identify a local resource.
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            startActivity(intent);
        }
    }


    private void initImageButton() {
        ImageButton ib = findViewById(R.id.imageContact);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.CAMERA)) {
                            Snackbar.make(findViewById(R.id.activity_main), "The app needs permission to take pictures.",
                                    Snackbar.LENGTH_INDEFINITE).setAction("Ok", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[] {Manifest.permission.CAMERA},
                                            PERMISSION_REQUEST_CAMERA);
                                }
                            })
                            .show();

                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.CAMERA},
                                    PERMISSION_REQUEST_CAMERA);
                        }
                    } else {
                        takePhoto();
                    }
                } else {
                    takePhoto();
                }
            }
        });
    }





    private void initListButton() {
        ImageButton ibList = findViewById(R.id.imageButtonList);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ContactListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initMapButton() {
        ImageButton ibList = findViewById(R.id.imageButtonMap);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ContactMapActivity.class);
                // checks whether the contact has an ID. If not, a message is posted for the user.
                // If there is an ID, that ID is passed to the ContactMapActivity
                if (currentContact.getContactID() == -1) {
                    Toast.makeText(getBaseContext(), "Contact must be saved before it can be mapped",
                            Toast.LENGTH_LONG).show();
                } else {
                    intent.putExtra("contactid", currentContact.getContactID());
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initSettingsButton() {
        ImageButton ibList = findViewById(R.id.imageButtonSettings);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ContactSettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initToggleButton() {
        final ToggleButton editToggle = findViewById(R.id.toggleButtonEdit);
        editToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setForEditing(editToggle.isChecked());
            }
        });
    }

    private void setForEditing(boolean enabled) {
        EditText editName = findViewById(R.id.editName);
        EditText editAddress = findViewById(R.id.editAddress);
        EditText editCity = findViewById(R.id.editCity);
        EditText editState = findViewById(R.id.editState);
        EditText editZipCode = findViewById(R.id.editZipcode);
        EditText editPhone = findViewById(R.id.editHome);
        EditText editCell = findViewById(R.id.editCell);
        EditText editEmail = findViewById(R.id.editEMail);
        Button buttonChange = findViewById(R.id.buttonBirthday);
        Button buttonSave = findViewById(R.id.buttonSave);
        ImageButton picture = findViewById(R.id.imageContact);
        picture.setEnabled(enabled);
        editName.setEnabled(enabled);
        editAddress.setEnabled(enabled);
        editCity.setEnabled(enabled);
        editState.setEnabled(enabled);
        editZipCode.setEnabled(enabled);
        //editPhone.setEnabled(enabled);
        //editCell.setEnabled(enabled);
        editEmail.setEnabled(enabled);
        buttonChange.setEnabled(enabled);
        buttonSave.setEnabled(enabled);

        // when editing is turned on, the call function works. in viewing mode, it does not.
        // This is because you disabled the EditTexts in viewing mode to avoid making any
        // accidental changes to the user’s information. To correct this, you need to modify the
        // setForEditing method in MainActivity.java.

        // An EditText has to be enabled to allow it to respond to a long-click event. This means
        // that you cannot ever disable them. Delete the setEnabled lines of code associated with
        // the editPhone and editCell variables. The problem with doing this is that now the phone
        // number will be editable, even in viewing mode. To correct this problem, you need to set
        // the inputType of the EditText to null when in viewing mode, and set it back to
        // accepting phone numbers when in editing mode, using the below changes to if/else block

        if (enabled) {
            editName.requestFocus();
            editPhone.setInputType(InputType.TYPE_CLASS_PHONE);
            editCell.setInputType(InputType.TYPE_CLASS_PHONE);
        }
        else {
            ScrollView s = findViewById(R.id.scrollView);
            s.fullScroll(ScrollView.FOCUS_UP);
            editPhone.setInputType(InputType.TYPE_NULL);
            editCell.setInputType(InputType.TYPE_NULL);
        }

    }

    private void initChangeDateButton() {
        Button changeDate = findViewById(R.id.buttonBirthday);
        changeDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                FragmentManager fm = getSupportFragmentManager();
                DatePickerDialog datePickerDialog = new DatePickerDialog();
                datePickerDialog.show(fm, "DatePick");
            }
        });
    }

    private void initTextChangedEvents(){
        final EditText etContactName = findViewById(R.id.editName);
        etContactName.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                currentContact.setContactName(etContactName.getText().toString());
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                //  Auto-generated method stub
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //  Auto-generated method stub
            }
        });

        final EditText etStreetAddress = findViewById(R.id.editAddress);
        etStreetAddress.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                currentContact.setStreetAddress(etStreetAddress.getText().toString());
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                //  Auto-generated method stub
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //  Auto-generated method stub
            }
        });

        final EditText etCity = findViewById(R.id.editCity);
        etCity.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable editable) {
                currentContact.setCity(etCity.getText().toString());
            }
        });

        final EditText etState = findViewById(R.id.editState);
        etState.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable editable) {
                currentContact.setState(etState.getText().toString());
            }
        });

        final EditText etZip = findViewById(R.id.editZipcode);
        etZip.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable editable) {
                currentContact.setZipCode(etZip.getText().toString());
            }
        });

        final EditText etPhone = findViewById(R.id.editHome);
        etPhone.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                currentContact.setPhoneNumber(etPhone.getText().toString());
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                //  Auto-generated method stub
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //  Auto-generated method stub
            }
        });

        final EditText etCell = findViewById(R.id.editCell);
        etCell.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
             }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable editable) {
                currentContact.setCellNumber(etCell.getText().toString());
            }
        });

        final EditText etEMail = findViewById(R.id.editEMail);
        etEMail.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable editable) {
                currentContact.setEMail(etEMail.getText().toString());
            }
        });

        etPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        etCell.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

    }

    private void initSaveButton() {
        Button saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                boolean wasSuccessful;
                hideKeyboard();
                ContactDataSource ds = new ContactDataSource(MainActivity.this);
                try {
                    ds.open();

                    if (currentContact.getContactID() == -1) {
                        wasSuccessful = ds.insertContact(currentContact);
                        if (wasSuccessful) {
                            int newId = ds.getLastContactId();
                            currentContact.setContactID(newId);
                        }

                    }
                    else {
                        wasSuccessful = ds.updateContact(currentContact);
                    }
                    ds.close();
                }
                catch (Exception e) {
                    wasSuccessful = false;
                }

                if (wasSuccessful) {
                    ToggleButton editToggle = findViewById(R.id.toggleButtonEdit);
                    editToggle.toggle();
                    setForEditing(false);
                }
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText editName = findViewById(R.id.editName);
        imm.hideSoftInputFromWindow(editName.getWindowToken(), 0);
        EditText editAddress = findViewById(R.id.editAddress);
        imm.hideSoftInputFromWindow(editAddress.getWindowToken(), 0);
        EditText et = findViewById(R.id.editCity);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et = findViewById(R.id.editState);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et = findViewById(R.id.editZipcode);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et = findViewById(R.id.editHome);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et = findViewById(R.id.editCell);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et = findViewById(R.id.editEMail);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    //method to retrieve contact and populate layout with retrieved values
    private void initContact(int id) {
        //retrieve contact and assign to activity's currentContact variable
        ContactDataSource ds = new ContactDataSource(MainActivity.this);
        try {
            ds.open();
            currentContact = ds.getSpecificContact(id);
            ds.close();
        }
        catch (Exception e) {
            Toast.makeText(this, "Load Contact Failed", Toast.LENGTH_LONG).show();
        }
        //refs to all widgets in layout so contact's data is displayed in them
        EditText editName = findViewById(R.id.editName);
        EditText editAddress = findViewById(R.id.editAddress);
        EditText editCity = findViewById(R.id.editCity);
        EditText editState = findViewById(R.id.editState);
        EditText editZipCode = findViewById(R.id.editZipcode);
        EditText editPhone = findViewById(R.id.editHome);
        EditText editCell = findViewById(R.id.editCell);
        EditText editEmail = findViewById(R.id.editEMail);
        TextView birthDay = findViewById(R.id.textBirthday);

        // widgets set to display the values in retrieved contact
        editName.setText(currentContact.getContactName());
        editAddress.setText(currentContact.getStreetAddress());
        editCity.setText(currentContact.getCity());
        editState.setText(currentContact.getState());
        editZipCode.setText(currentContact.getZipCode());
        editPhone.setText(currentContact.getPhoneNumber());
        editCell.setText(currentContact.getCellNumber());
        editEmail.setText(currentContact.getEMail());
        birthDay.setText(DateFormat.format("MM/dd/yyyy",
                currentContact.getBirthday().getTimeInMillis()).toString());

        // display the retrieved picture along with the rest of the contact’s data
        ImageButton picture = (ImageButton) findViewById(R.id.imageContact);
        if (currentContact.getPicture() != null) {
            picture.setImageBitmap(currentContact.getPicture());
        } else {
            picture.setImageResource(R.drawable.photoicon);
        }
        // gets a reference to the ImageButton on the layout and checks if the contact has a picture.
        // If there is a picture, it sets it as the button’s image.
        // If not, the default image resource is displayed.

    }


    @Override
    public void didFinishDatePickerDialog(Calendar selectedTime) {
        TextView birthDay = findViewById(R.id.textBirthday);
        birthDay.setText(DateFormat.format("MM/dd/yyyy", selectedTime));
        currentContact.setBirthday(selectedTime);
    }


    private void initMessagingFunction() {
        EditText editPhone = (EditText) findViewById(R.id.editHome);
        editPhone.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                checkMsgPermission(currentContact.getPhoneNumber());
                return false;
            }
        });

        EditText editCell = (EditText) findViewById(R.id.editCell);
        editCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                checkMsgPermission(currentContact.getCellNumber());
                return false;
            }
        });
    }



    private void checkMsgPermission(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.SEND_SMS)) {

                    Snackbar.make(findViewById(R.id.activity_main),
                            "MyContactList requires this permission to send SMS message from the app.",
                            Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {

                        @Override
                        public void onClick (View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.SEND_SMS },
                                    PERMISSION_REQUEST_SMS);
                        }
                    }).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                                    String[]{Manifest.permission.SEND_SMS},
                            PERMISSION_REQUEST_SMS);
                }
            } else {
                msgContact(phoneNumber);
            }
        } else {
            msgContact(phoneNumber);
        }
    }


    private void msgContact(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            startActivity(intent);
        }
    }




}
