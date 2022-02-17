package com.example.mycontactlist;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContactMapActivity extends AppCompatActivity implements OnMapReadyCallback {
// The OnMapReadyCallback interface is used to notify the activity that the map has been downloaded
// and is ready to be used and allows it to work with the map


    final int PERMISSION_REQUEST_LOCATION = 101; // declares a constant to identify the permission that is being requested
    GoogleMap gMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    ArrayList<Contact> contacts = new ArrayList<>();
    Contact currentContact = null;

    // get the data for mapping
    // This is done by checking for any extras.
    // If there are no extras, all the contacts are retrieved.
    // If there is an extra, just the information for one contact is retrieved

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_map);

        // code to retrieve contact information
        Bundle extras = getIntent().getExtras();
        try {
            ContactDataSource ds = new ContactDataSource(ContactMapActivity.this);
            ds.open();
            if (extras != null) {
                currentContact = ds.getSpecificContact(extras.getInt("contactid"));
            } else {
                contacts = ds.getContacts("contactname", "ASC");
            }
            ds.close();
        }
        catch (Exception e) {
            Toast.makeText(this, "Contact(s) could not be retrieved.", Toast.LENGTH_LONG).show();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // A reference to the FusedLocationProviderClient is assigned to the fusedLocationProviderClient
        // variable so that these system services are available to the ContactMapActivity.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // A reference to the map fragment in the layout is assigned to the mapFragment variable
        // SupportMapFragment is used to make the map compatible with earlier versions of Android OS
        mapFragment.getMapAsync(this);
        // The map is retrieved asynchronously, then onMapReady() method is executed and we can begin working with the map.



        createLocationRequest(); // calls a method that sets up the location listener
        createLocationCallback(); // calls a method to process any location changes provided by the FusedLocationProviderClient

        initListButton();
        initSettingsButton();
        initMapButton();
        initMapTypeButtons();

    }


    // LocationManager obj is sent the msg removeUpdates to end listening to the gpsListener.
    // use try/catch bc the activity could pause before the user presses the Get Location button.
    // In that case, neither locationManager nor gpsListener would have values, so app would crash
    // The first line calls the overridden method to execute the standard onPause routine for the activity
    @Override
    public void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }

//        try {
//            locationManager.removeUpdates(gpsListener);
//            locationManager.removeUpdates(networkListener);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    private void initListButton() {
        ImageButton ibList = findViewById(R.id.imageButtonList);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ContactMapActivity.this, ContactListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initSettingsButton() {
        ImageButton ibList = findViewById(R.id.imageButtonSettings);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ContactMapActivity.this, ContactSettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initMapButton() {
        ImageButton ibList = findViewById(R.id.imageButtonMap);
        ibList.setEnabled(false);
    }


    @Override // method signature required to override the super class method of the same name
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // request response sends the requestCode, which is the value you sent the request with PERMISSION_REQUEST_LOCATION
        // It is used to determine with which permission request it is dealing. The response also sends a
        // string array with the permissions requested and an integer array with the user response to each request.

        switch (requestCode) { // switch used to determine to which permission the method is responding.
            // In our activity, we only have one permission request, so there is only one case statement.
            // This method can be used to deal with multiple permission requests by simply adding additional case statements.
            case PERMISSION_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(ContactMapActivity.this, "MyContactList will not locate your contacts.",
                            Toast.LENGTH_LONG).show();
                } // if statement uses PackageManager to check if the permission was granted by the user.
                // PackageManager is an operating system object that keeps track of what permissions
                // were granted by the user for the apps on the device. If the permission was granted,
                // the app starts finding the location. If not, a message explains how the app will function without the permission.
            }
        }
    }


    private void createLocationRequest() {
        // creates the parameters of your location request.
        // It sets the standard monitoring interval (in milliseconds), the fastest interval needed,
        // and the required accuracy level of the reported location. By using PRIORITY_HIGH_ACCURACY,
        // we use the device’s GPS to get the coordinates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        // responds to location changes provided by the FusedLocationProviderClient.
        // If there are no changes or the LocationResult does not have any locations in it
        // due to an error, then nothing is done. If there are location results, then it loops
        // through the set of results, displaying them as a Toast on the screen
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Toast.makeText(getBaseContext(), "Lat: " + location.getLatitude() +
                            " Long: " + location.getLongitude() +
                            " Accuracy: " + location.getAccuracy(), Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private void startLocationUpdates() {
        // if block checks if the device is running SDK 23 or higher and location permission has not been explicitly granted.
        // If both conditions are true, the code is exited and the app will not be able to access the devices location
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        // starts the location listening through the FusedLocationProviderClient
        // enables the map to find the device location and places a button on the map that allows
        // the user to enable or disable the display of the small blue dot on the map
        // (which represents the device’s location) and to zoom to that location
        gMap.setMyLocationEnabled(true);
    }

    private void stopLocationUpdates() {
        // stops location updates from the FusedLocationProviderClient like the start method above
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // overrides a super class method
        // Since we want to use location in the map, we will add the permission request in this method
        // This code receives the GoogleMap object and assigns it to our gMap variable.
        // The map type is also set. A map_type_normal is a standard highway map.
        // Other valid types include MAP_TYPE_SATELLITE for satellite pictures and
        // MAP_TYPE_TERRAIN for a map of the terrain features
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        RadioButton rbNormal = findViewById(R.id.radioButtonNormal);
        rbNormal.setChecked(true);

        //code to place markers on map at contact location
        Point size = new Point();
        WindowManager w = getWindowManager();
        w.getDefaultDisplay().getSize(size);
        int measuredWidth = size.x;
        int measuredHeight = size.y;

        if (contacts.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i=0; i<contacts.size(); i++) {
                currentContact = contacts.get(i);
                Geocoder geo = new Geocoder(this);
                List<Address> addresses = null;

                String address = currentContact.getStreetAddress() + ", " +
                        currentContact.getCity() + ", " +
                        currentContact.getState() + " " +
                        currentContact.getZipCode();

                try {
                    addresses = geo.getFromLocationName(address, 1);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                LatLng point = new LatLng(addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude());
                builder.include(point);

                gMap.addMarker(new MarkerOptions().position(point).title(
                        currentContact.getContactName()).snippet(address));
            }
            gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),
                    measuredWidth, measuredHeight, 450));
        } else {
            if (currentContact != null) {
                Geocoder geo = new Geocoder(this);
                List<Address> addresses = null;

                String address = currentContact.getStreetAddress() + ", " +
                        currentContact.getCity() + ", " +
                        currentContact.getState() + " " +
                        currentContact.getZipCode();

                try {
                    addresses = geo.getFromLocationName(address, 1);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                LatLng point = new LatLng(addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude());

                gMap.addMarker(new MarkerOptions().position(point).title(currentContact.getContactName()).snippet(address));
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16));
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(ContactMapActivity.this).create();
                alertDialog.setTitle("No Data");
                alertDialog.setMessage("No data is available for the mapping function.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    //@Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        finish();
                    }
                });
                alertDialog.show();
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= 23) { // If SDK is less than 23, the app starts the code to find the device’s location.
                if (ContextCompat.checkSelfPermission(ContactMapActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) { // If SDK > 22, checks if permission has been previously granted.
                    // If it has, the app starts the code to find the device’s location

                    // if block determines if the user had previously denied a request from the app to access the device’s location.
                    // If it has previously been denied, the app presents the user with a rationale informing them why the app needs this permission.
                    // The first time the app requests a permission, a “canned” request is made that just asks for the desired permission.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(ContactMapActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {

                        // This code asks for permission to access location if it was previously denied.
                        // It will only show the permission request if the user previously denied this specific permission
                        // Snackbar implements an AlertDialog that is run as an asynchronous task floating in the activity’s layout
                        Snackbar.make(findViewById(R.id.activity_contact_map),
                                "MyContactList requires this permission to locate your contacts",
                                Snackbar.LENGTH_INDEFINITE).setAction("OK",
                                new View.OnClickListener() {
                                    // LENGTH_INDEFINITE code keeps the Snackbar open until the user
                                    // explicitly dismisses it by clicking either the OK or Deny buttons.
                                    // create OnClick listener for the OK button click event

                                    @Override
                                    public void onClick (View view) {
                                        // requests permission for access to location
                                        // only asks for permission to access fine location
                                        // If granted, coarse location is also automatically granted
                                        ActivityCompat.requestPermissions(
                                                ContactMapActivity.this,
                                                new String[] {
                                                        Manifest.permission.ACCESS_FINE_LOCATION},
                                                PERMISSION_REQUEST_LOCATION);
                                    }
                                })
                                .show();
                    } else { // executed if the user has not previously denied access to location
                        ActivityCompat.requestPermissions(ContactMapActivity.this, new
                                        String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_LOCATION);
                    }
                } else {
                    startLocationUpdates();
                }
            } else {
                startLocationUpdates();
            }
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Error requesting permission", Toast.LENGTH_LONG).show();
        }

    }


    private void initMapTypeButtons() {
        RadioGroup rgMapType = findViewById(R.id.radioGroupMapType);
        rgMapType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rbNormal = findViewById(R.id.radioButtonNormal);
                if (rbNormal.isChecked()) {
                    gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else {
                    gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
            }
        });

    }




}
