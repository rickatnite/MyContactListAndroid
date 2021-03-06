package com.example.mycontactlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ContactListActivity extends AppCompatActivity {
    RecyclerView contactList;
    ContactAdapter contactAdapter;
    ArrayList<Contact> contacts; //hold contact objects instead of names

    //new instance of listener to pass to adapter
    private View.OnClickListener onItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) { //ref to viewHolder that produced the click with getTag
            RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
            int position = viewHolder.getAdapterPosition(); //use viewHolder to get position in the list to get corresponding Contact obj
            int contactId = contacts.get(position).getContactID(); //position value is index of item clicked in list
            Intent intent = new Intent(ContactListActivity.this, MainActivity.class);
            intent.putExtra("contactId", contactId); //puts contactId in bundle passed to mainActivity
            startActivity(intent); //starts MainActivity by clicking any contact in list
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        initListButton();
        initMapButton();
        initSettingsButton();
        initAddContactButton();
        initDeleteSwitch();

        // BroadcastReceiver receives Intents and has the code used to respond to the Intent.
        // An Intent is broadcast from other apps or objects executing on the device.
        BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // The Intent concerning battery status sent by the OS contains information
                // about the battery as Extras. This line gets the extra associated with the
                // battery???s current charge level. Although the value is retrieved as an integer,
                // it is assigned to a double variable so that it can be used as a double later.
                double batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                // The extra associated with the scale used for measuring the charge is retrieved
                // and assigned to a double variable. Capturing the scale is important because
                // different devices may use different scales for measuring the charge.
                double levelScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                // The percentage of battery charge left is calculated by dividing the level
                // by the scale. If these two variables were not defined as doubles, this
                // calculation would produce incorrect results because a divide operation needs
                // to produce a double value. The result of the calculation is a number
                // between 0 and 1, which is multiplied by 100 to get a percentage. The floor
                // function is applied to take on the integer value of the result.
                int batteryPercent = (int) Math.floor(batteryLevel / levelScale * 100);
                TextView textBatteryState = (TextView) findViewById(R.id.textBatteryLevel);
                textBatteryState.setText(batteryPercent + "%");
            }
        };
        // this IntentFilter listens for Intents that have been broadcast by the system and only
        // lets through the ones the developer is looking for. In this case, the filter looks for
        // Battery Status changed intent. This is required because a BroadcastReceiver can respond
        // to any intent. However, you want it to respond only to Intents sent by the battery.
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // BroadcastReceiver is registered, which means that the app is told to listen for battery
        // status intents and handle them with the BroadcastReceiver defined in the activity.
        registerReceiver(batteryReceiver, filter);

    }

    @Override
    public void onResume() {
        super.onResume();

        //retrieve store user preferences
        //below code needs to be in onResume instead of onCreate bc it is executed every time the user navigates to the activity
        String sortBy = getSharedPreferences("MyContactListPreferences", Context.MODE_PRIVATE).getString("sortfield", "contactname");
        String sortOrder = getSharedPreferences("MyContactListPreferences", Context.MODE_PRIVATE).getString("sortorder", "ASC");

        //create new datasource object, open db, retrieve contact names with getContacts, close db
        ContactDataSource ds = new ContactDataSource(this);
        try {
            ds.open();
            contacts = ds.getContacts(sortBy, sortOrder);
            ds.close();
            if (contacts.size() > 0) { //setup recyclerView to display data
                contactList = findViewById(R.id.rvContacts); //ref the widget and create instance of LayoutManager to display indv items
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this); //LLM displays vertical scrolling list
                contactList.setLayoutManager(layoutManager); //pass arraylist of contact names
                contactAdapter = new ContactAdapter(contacts, this); //instance of ContactAdapter
                contactAdapter.setOnItemClickListener(onItemClickListener);
                contactList.setAdapter(contactAdapter);//associate adapter with recyclerView
            }// if statement to check if contacts are retrieved from db
            else { //opens mainActivity if there are no contacts
                Intent intent = new Intent(ContactListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
        catch (Exception e) { //toast displays message and makeText method configures message
            Toast.makeText(this, "Error retrieving contacts", Toast.LENGTH_LONG).show();
        }//params: (where to display, sets message, how long message should display) - show() displays msg

    }
    private void initAddContactButton() {
        Button newContact = findViewById(R.id.buttonAddContact);
        newContact.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ContactListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initDeleteSwitch() {
        Switch s = findViewById(R.id.switchDelete); //assign switch ref and create listener
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                boolean status = compoundButton.isChecked(); //check switch status - on=true - off=false
                contactAdapter.setDelete(status); //status passed to adapter
                contactAdapter.notifyDataSetChanged(); //tells adapter to redraw the list
            }// if switch is on, delete button will be displayed
        });
    }


    private void initListButton() {
        ImageButton ibList = findViewById(R.id.imageButtonList);
        ibList.setEnabled(false);
    }

    private void initSettingsButton() {
        ImageButton ibList = findViewById(R.id.imageButtonSettings);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ContactListActivity.this, ContactSettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initMapButton() {
        ImageButton ibList = findViewById(R.id.imageButtonMap);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ContactListActivity.this, ContactMapActivity.class);
                // checks whether the contact has an ID. If not, a message is posted for the user.
                // If there is an ID, that ID is passed to the ContactMapActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }



}
























