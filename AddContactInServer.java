package com.example.ablejobssynccontacts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference contactsServerDBRef = db.collection("contactsDB");

    private SharedPreferences sharedPref;
    private int lastWrittenContactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_in_server);
        setTitle("Add contact in Server DB");

        nameEditText = findViewById(R.id.edit_text_name);
        phoneEditText = findViewById(R.id.edit_text_phone);
        addContactButton = findViewById(R.id.add_contact_button);
        sharedPref = getSharedPreferences("com.example.ablejobssynccontacts.lastWrittenContactId"
                , Context.MODE_PRIVATE);

        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastWrittenContactId = sharedPref.getInt("last_written_contact_id", 0);
                String name = nameEditText.getText().toString();
                String phone = phoneEditText.getText().toString();

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(getApplicationContext(),
                            "Please enter valid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> mp = new HashMap<>();
                mp.put("Contact_id", lastWrittenContactId + 1);
                mp.put("Name", name);
                mp.put("Phone", phone);

                contactsServerDBRef.document().set(mp)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(),
                                "Contact added successfully on Server DB", Toast.LENGTH_SHORT).show();
                        sharedPref.edit().putInt("last_written_contact_id", ++lastWrittenContactId).apply();
                        nameEditText.setText("");
                        phoneEditText.setText("");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),
                                "Oops !! something went wrong ....", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Error writing document", e);
                    }
                });

            }
        });
    }
}
