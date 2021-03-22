package com.example.ablejobssynccontacts;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SyncContactsActivity extends AppCompatActivity {
    private static final String TAG = "SyncContactsActivity";

    private static final String[] permissions = {Manifest.permission.WRITE_CONTACTS};

    private static final int CONTACTS_WRITE_PERMISSIONS_CODE = 100;
    private static final String KEY_TIME_STAMP = "Time_stamp";
    private static final String KEY_NAME = "Name";
    private static final String KEY_PHONE = "Phone";
    private static final String KEY_LAST_SYNC_TIME = "Last_sync_time";

    private static final String DOC_LAST_SYNCED_INFO = "LastSyncedInfo";
    private static final String CONTACTS_DB = "contactsDB";

    private Button addContactsInServerDBButton;
    private Button syncContactsButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference contactsServerDBRef = db.collection(CONTACTS_DB);

    private long lastSyncTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_contacts_activity_layout);
        setTitle("Able Sync Contacts");

        checkPermission(permissions, CONTACTS_WRITE_PERMISSIONS_CODE);

        addContactsInServerDBButton = findViewById(R.id.add_server_db_contacts_button);
        syncContactsButton = findViewById(R.id.sync_contacts_button);
        progressBar = findViewById(R.id.progress_bar);
        setProgBarVisibility(false);

        createButtonListeners();
    }

    private void createButtonListeners() {

        syncContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                contactsServerDBRef.document(DOC_LAST_SYNCED_INFO).get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            if (doc.exists()) {
                                lastSyncTime = doc.getLong(KEY_LAST_SYNC_TIME);
                            } else {
                                lastSyncTime = 0;
                            }

                            fetchNewContactsAndStoreInDB();

                        }
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

    private void fetchNewContactsAndStoreInDB() {
        contactsServerDBRef.whereGreaterThan(KEY_TIME_STAMP, lastSyncTime)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                if (task.isSuccessful()) {
                    int totalCnt = task.getResult().size();

                    if (totalCnt == 0) {
                        showToast("Contacts already synced ...");
                        return;
                    }

                    initProgressBar(totalCnt);

                    long maxTimeStamp = 0;
                    int insertCnt = 0;
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                        long timeStamp = document.getLong(KEY_TIME_STAMP);
                        String displayName = document.getString(KEY_NAME);
                        String phoneNumber = document.getString(KEY_PHONE);

                        if (insertContactInLocalDB(displayName, phoneNumber)) {
                            if (timeStamp > maxTimeStamp) {
                                maxTimeStamp = timeStamp;
                            }
                            progressBar.setProgress(++insertCnt);
                        }
                    }

                    showToast("New contacts synced with local contacts db.");

                    lastSyncTime = maxTimeStamp;
                    Map<String, Object> mp = new HashMap<>();
                    mp.put(KEY_LAST_SYNC_TIME, lastSyncTime);
                    contactsServerDBRef.document(DOC_LAST_SYNCED_INFO).set(mp);
                } else {
                    showToast("Oops !!! Error getting contacts ...");
                    Log.d(TAG, "Oops !!! Error getting contacts ...", task.getException());
                }

                setProgBarVisibility(false);
            }
        });
    }

    boolean insertContactInLocalDB(String displayName, String phoneNumber) {
        Uri addContactsUri = ContactsContract.Data.CONTENT_URI;
        long rowContactId = getRawContactId();

        if (insertContactDisplayName(addContactsUri, rowContactId, displayName) != null
                && insertContactPhoneNumber(addContactsUri, rowContactId, phoneNumber) != null) {
            return true;
        } else return false;
    }

    private void initProgressBar(int max) {
        progressBar.setProgress(0);
        progressBar.setMax(max);
        setProgBarVisibility(true);
    }

    private void setProgBarVisibility(boolean visibility) {
        progressBar.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CONTACTS_WRITE_PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                finish();
            }
        }
    }

}
