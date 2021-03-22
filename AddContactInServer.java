package com.example.ablejobssynccontacts;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddContactInServer extends AppCompatActivity {
    private static final String TAG = "AddContactInServer";

    private EditText nameEditText;
    private EditText phoneEditText;
    private Button addContactButton;

    private static final String KEY_TIME_STAMP = "Time_stamp";
    private static final String KEY_NAME = "Name";
    private static final String KEY_PHONE = "Phone";

    private static final String CONTACTS_DB = "contactsDB";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference contactsServerDBRef = db.collection(CONTACTS_DB);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_in_server);
        setTitle("Add contact in Server DB");

        nameEditText = findViewById(R.id.edit_text_name);
        phoneEditText = findViewById(R.id.edit_text_phone);
        addContactButton = findViewById(R.id.add_contact_button);

        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long timeStamp = System.currentTimeMillis();
                String name = nameEditText.getText().toString();
                String phone = phoneEditText.getText().toString();

                if (name.isEmpty() || phone.isEmpty()) {
                    showToast("Please enter valid data");
                    return;
                }

                Map<String, Object> mp = new HashMap<>();
                mp.put(KEY_TIME_STAMP, timeStamp);
                mp.put(KEY_NAME, name);
                mp.put(KEY_PHONE, phone);

                contactsServerDBRef.document().set(mp)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                showToast("Contact added successfully on Server DB");
                                nameEditText.setText("");
                                phoneEditText.setText("");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("Oops !! something went wrong ....");
                        Log.w(TAG, "Error writing document", e);
                    }
                });

            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
