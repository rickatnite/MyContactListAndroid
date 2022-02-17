package com.example.mycontactlist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class ContactAdapter extends RecyclerView.Adapter{
    private ArrayList<Contact> contactData;
    private View.OnClickListener mOnItemClickListener; //declares private variable to hold OnClickListener obj passed from activity
    private boolean isDeleting; //these variables hold delete status of adapter and activity (context) it is operating in
    private Context parentContext; //context is needed to open the db so contact can be deleted and display msg if it fails

    //declare behavior of viewHolder class owned by this adapter
    public class ContactViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewContact;
        public TextView textPhone;
        public Button deleteButton;
        public ContactViewHolder(@NonNull View itemView) { //need nonNull - cannot return null value
            super(itemView); //declare constructor method
            textViewContact = itemView.findViewById(R.id.textContactName); //get refs for textViews/button
            textPhone = itemView.findViewById(R.id.textPhoneNumber);
            deleteButton = itemView.findViewById(R.id.buttonDeleteContact);
            itemView.setTag(this); //sets tag to identify which item is clicked
            itemView.setOnClickListener(mOnItemClickListener); //sets viewHolder's OnClickListener to listener passed from activity
        }

        //these are used by adapter to return the textViews to set and change the displayed text
        public TextView getContactTextView() {
            return textViewContact;
        }
        public TextView getPhoneTextView() {
            return textPhone;
        }
        public Button getDeleteButton() {
            return deleteButton;
        }
    }

    //declare constructor method for adapter to associate data to be displayed
    public ContactAdapter(ArrayList<Contact> arrayList, Context context) {
        contactData = arrayList;
        parentContext = context;
    } //context needs to be passed to the adapter's constructor method from the activity and held in new variable

    //sets up adapter method to pass listener from activity to adapter
    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        mOnItemClickListener = itemClickListener;
    }

    //method is called for each item in the data set to be displayed - creates visual display for each item using layout
    //a viewHolder is created for each item using the inflated xml and returned to RecyclerView to be displayed by activity
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ContactViewHolder(v);
    }

    //called for each item in the data set and passed to viewHolder created by onCreateViewHolder
    //then it is cast into the associated ContactViewHolder
    //getContactTextView method is called to set text attribute to the name of the contact at current position
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) { //final so onClick can access its value
        ContactViewHolder cvh = (ContactViewHolder) holder;
        cvh.getContactTextView().setText(contactData.get(position).getContactName()); //name and phone number position used to retrieve contact from arraylist
        cvh.getPhoneTextView().setText(contactData.get(position).getPhoneNumber()); //contact obj methods used to get required values
        if (isDeleting) { //if adapter is in delete mode, delete button for each contact is set to visible
            cvh.getDeleteButton().setVisibility(View.VISIBLE);
            cvh.getDeleteButton().setOnClickListener(new View.OnClickListener() { //calls adapter's delete method
                @Override
                public void onClick(View view) {
                    deleteItem(position);
                }//delete method is passed to position of contact in data so proper contact can be deleted
            });
        }
        else {
            cvh.getDeleteButton().setVisibility(View.INVISIBLE);
        }
    }

    //returns number of items in the dataset to determine how many times to execute the other 2 methods
    @Override
    public int getItemCount() {
        return contactData.size();
    }

    private void deleteItem(int position) {
        Contact contact = contactData.get(position);//get selected contact from arrayList
        ContactDataSource ds = new ContactDataSource(parentContext);
        try { //create and open new CDS obj, delete contact with deleteContact method
            ds.open();
            boolean didDelete = ds.deleteContact(contact.getContactID());
            ds.close();
            if (didDelete) { //if successful, contact is removed from adapter's data
                contactData.remove(position);
                notifyDataSetChanged(); //adapter is told to refresh display
            }
            else {
                Toast.makeText(parentContext, "Delete Failed!", Toast.LENGTH_LONG).show();
            }

        }
        catch (Exception e) {

        }
    }

    //method to allow delete switch on activity to set the adapter to delete mode
    public void setDelete(boolean b) {
        isDeleting = b;
    }
}
