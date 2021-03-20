package com.example.ablejobssynccontacts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class SyncContactsActivity extends AppCompatActivity {
    private static final String TAG = "SyncContactsActivity";

    private static final String[] permissions = {Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS};
    private static final int CONTACTS_READ_WRITE_PERMISSIONS_CODE = 100;

    private Button addContactsInServerDBButton;
    private Button syncContactsButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference contactsServerDBRef = db.collection("contactsDB");

    private SharedPreferences sharedPref;
    private int lastSyncedContactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_contacts_activity_layout);
        setTitle("Able Sync Contacts");

        checkPermission(permissions, CONTACTS_READ_WRITE_PERMISSIONS_CODE);

        addContactsInServerDBButton = findViewById(R.id.add_server_db_contacts_button);
        syncContactsButton = findViewById(R.id.sync_contacts_button);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        sharedPref = getSharedPreferences("com.example.ablejobssynccontacts.lastSyncedContactId"
                , Context.MODE_PRIVATE);

        createButtonListeners();
    }

    private void createButtonListeners() {
        syncContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastSyncedContactId = sharedPref.getInt("last_synced_contact_id", 0);

                contactsServerDBRef.whereGreaterThan("Contact_id", lastSyncedContactId)
                        .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if (task.isSuccessful()) {
                            int totalCnt = task.getResult().size();

                            if (totalCnt == 0) {
                                Toast.makeText(getApplicationContext(),
                                        "Contacts already synced ...", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            progressBar.setProgress(0);
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setMax(totalCnt);

                            int insertCnt = 0;
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                String displayName = document.getString("Name");
                                String phoneNumber = document.getString("Phone");

                                if (insertContactInLocalDB(displayName, phoneNumber)) {
                                    lastSyncedContactId++;
                                    progressBar.setProgress(++insertCnt);
                                }
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Oops !!! Error getting contacts ...", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Oops !!! Error getting contacts ...", task.getException());
                        }

                        sharedPref.edit().putInt("last_synced_contact_id", lastSyncedContactId).apply();
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        addContactsInServerDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddContactInServer.class);
                startActivity(intent);
            }
        });
    }

    boolean insertContactInLocalDB(String displayName, String phoneNumber) {
        Uri addContactsUri = ContactsContract.Data.CONTENT_URI;
        long rowContactId = getRawContactId();

        if (insertContactDisplayName(addContactsUri, rowContactId, displayName) != null
                && insertContactPhoneNumber(addContactsUri, rowContactId, phoneNumber) != null) {
            Toast.makeText(getApplicationContext(),
                    "New contacts synced with local contacts db.", Toast.LENGTH_SHORT).show();
            return true;
        } else return false;
    }

    private long getRawContactId() {
        ContentValues contentValues = new ContentValues();
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        assert rawContactUri != null;
        long ret = ContentUris.parseId(rawContactUri);
        return ret;
    }


    private Uri insertContactDisplayName(Uri addContactsUri, long rawContactId, String displayName) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName);

        return getContentResolver().insert(addContactsUri, contentValues);
    }

    private Uri insertContactPhoneNumber(Uri addContactsUri, long rawContactId, String phoneNumber) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);

        int phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneContactType);

        return getContentResolver().insert(addContactsUri, contentValues);
    }

    private void checkPermission(String[] permissions, int requestCode) {

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(SyncContactsActivity.this, permission)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(SyncContactsActivity.this,
                        new String[] { permission }, requestCode);
            }
        }
    }


}
