package com.example.universalyogaadminapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ViewBookedClassesActivity extends AppCompatActivity {

    private ListView listViewEmails;
    private SearchView searchViewEmails;
    private ArrayAdapter<String> emailAdapter;
    private ArrayList<String> emailList;
    private ArrayList<String> allEmails; // Full list of emails for filtering
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_booked_classes);

        // Enable the back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Booked Classes by Email");
        }

        // Initialize views
        listViewEmails = findViewById(R.id.listViewEmails);
        searchViewEmails = findViewById(R.id.searchViewEmails);
        emailList = new ArrayList<>();
        allEmails = new ArrayList<>();

        // Set up ArrayAdapter for the ListView
        emailAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emailList);
        listViewEmails.setAdapter(emailAdapter);

        // Firebase reference to "orders" node
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Load unique emails from Firebase
        loadEmailsFromFirebase();

        // Set up item click listener for ListView items
        listViewEmails.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmail = emailList.get(position);
            Intent intent = new Intent(ViewBookedClassesActivity.this, ViewClassesByEmailActivity.class);
            intent.putExtra("email", selectedEmail);
            startActivity(intent);
        });

        // Set up SearchView to filter emails
        setupSearchFunctionality();
    }

    // Handle the back button click in the Action Bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Finish the current activity to go back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadEmailsFromFirebase() {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> uniqueEmails = new HashSet<>();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String email = orderSnapshot.child("Email").getValue(String.class);
                    if (email != null) {
                        uniqueEmails.add(email);
                    }
                }

                // Update the email list and notify adapter
                allEmails.clear();
                allEmails.addAll(uniqueEmails);
                emailList.clear();
                emailList.addAll(allEmails);
                emailAdapter.notifyDataSetChanged();

                if (emailList.isEmpty()) {
                    Toast.makeText(ViewBookedClassesActivity.this, "No booked classes found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewBookedClassesActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchFunctionality() {
        searchViewEmails.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We handle filtering on text change, no need for submit action
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                emailList.clear();

                if (newText.isEmpty()) {
                    // If search is empty, show the full list
                    emailList.addAll(allEmails);
                } else {
                    // Filter emails that contain the search text
                    for (String email : allEmails) {
                        if (email.toLowerCase().contains(newText.toLowerCase())) {
                            emailList.add(email);
                        }
                    }
                }

                // Show a message if no results found
                if (emailList.isEmpty()) {
                    Toast.makeText(ViewBookedClassesActivity.this, "No matching email found.", Toast.LENGTH_SHORT).show();
                }

                emailAdapter.notifyDataSetChanged();
                return true;
            }
        });
    }
}
