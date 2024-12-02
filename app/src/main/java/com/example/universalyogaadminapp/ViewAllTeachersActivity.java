package com.example.universalyogaadminapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.content.ContentValues;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ViewAllTeachersActivity extends AppCompatActivity {

    private LinearLayout teacherListContainer;
    private YogaClassDatabaseHelper dbHelper;
    private AutoCompleteTextView searchTeacher;
    private ArrayList<String> teacherNames;
    private ArrayAdapter<String> adapter;
    private DatabaseReference teachersRef;

    // ActivityResultLauncher for teacher detail
    private final ActivityResultLauncher<Intent> teacherDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadTeachers();  // Reload teachers if the data changed
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_teachers);

        teacherListContainer = findViewById(R.id.teacherListContainer);
        searchTeacher = findViewById(R.id.searchTeacher);
        dbHelper = new YogaClassDatabaseHelper(this);
        teacherNames = new ArrayList<>();
        teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

        // Load all teachers and populate search suggestions
        loadTeachers();
        setupSearchFunctionality();

        // Back button functionality
        ImageView backIcon = findViewById(R.id.backIcon);
        backIcon.setOnClickListener(v -> finish());
    }

    // Load all teachers, and if none found locally, sync from Firebase
    private void loadTeachers() {
        Cursor cursor = dbHelper.getAllTeachers();

        if (cursor.getCount() == 0) {
            // If no teachers are found locally, sync from Firebase
            syncTeachersFromFirebase();
            return;
        }

        teacherListContainer.removeAllViews();
        teacherNames.clear();  // Clear the list of teacher names for suggestions

        while (cursor.moveToNext()) {
            int teacherId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_ID));
            String teacherName = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME));
            String teacherQualification = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION));

            teacherNames.add(teacherName);  // Add teacher name to the list for suggestions
            addTeacherCard(teacherId, teacherName, teacherQualification);
        }

        cursor.close();

        // Initialize and set the search adapter with teacher names after data is loaded
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teacherNames);
        searchTeacher.setAdapter(adapter);
    }

    // Sync teachers from Firebase to local SQLite database
    private void syncTeachersFromFirebase() {
        teachersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot teacherSnapshot : task.getResult().getChildren()) {
                    // Get teacher details from Firebase
                    String teacherName = teacherSnapshot.child("name").getValue(String.class);
                    String qualification = teacherSnapshot.child("qualification").getValue(String.class);
                    int age = teacherSnapshot.child("age").getValue(Integer.class);
                    String phone = teacherSnapshot.child("phone").getValue(String.class);
                    String address = teacherSnapshot.child("address").getValue(String.class);
                    String description = teacherSnapshot.child("description").getValue(String.class);

                    // Create ContentValues to insert teacher data into SQLite
                    ContentValues values = new ContentValues();
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME, teacherName);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION, qualification);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_AGE, age);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_PHONE, phone);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_ADDRESS, address);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER_DESCRIPTION, description);

                    dbHelper.addTeacher(values);  // Add teacher to local SQLite database
                }
                // Reload the teachers from the local database after syncing
                loadTeachers();
            } else {
                Toast.makeText(this, "No data found in Firebase or sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }



    // Set up search functionality
    private void setupSearchFunctionality() {
        // Set up a listener for item click in the search bar
        searchTeacher.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTeacherName = (String) parent.getItemAtPosition(position);
            searchAndDisplayTeachers(selectedTeacherName);
        });
    }

    // Search and display teachers by name
    private void searchAndDisplayTeachers(String teacherName) {
        Cursor cursor = dbHelper.searchTeacherByName(teacherName);

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No teachers found", Toast.LENGTH_SHORT).show();
            teacherListContainer.removeAllViews();
            return;
        }

        teacherListContainer.removeAllViews();

        while (cursor.moveToNext()) {
            int teacherId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME));
            String qualification = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION));

            addTeacherCard(teacherId, name, qualification);
        }

        cursor.close();
    }

    // Helper method to add a teacher card to the layout
    private void addTeacherCard(int teacherId, String name, String qualification) {
        LinearLayout teacherCard = new LinearLayout(this);
        teacherCard.setOrientation(LinearLayout.VERTICAL);
        teacherCard.setPadding(24, 24, 24, 24);
        teacherCard.setBackgroundResource(R.drawable.class_card_background);
        teacherCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) teacherCard.getLayoutParams();
        params.setMargins(0, 0, 0, 24);
        teacherCard.setLayoutParams(params);

        TextView teacherName = new TextView(this);
        teacherName.setText("Name: " + name);
        teacherName.setTextSize(18);
        teacherName.setTextColor(Color.BLACK);
        teacherCard.addView(teacherName);

        TextView teacherQualification = new TextView(this);
        teacherQualification.setText("Qualification: " + qualification);
        teacherQualification.setTextSize(18);
        teacherQualification.setTextColor(Color.BLACK);
        teacherCard.addView(teacherQualification);

        // Set an OnClickListener to the teacherCard to navigate to TeacherDetailActivity
        teacherCard.setOnClickListener(v -> {
            Intent intent = new Intent(ViewAllTeachersActivity.this, TeacherDetailActivity.class);
            intent.putExtra("teacherId", teacherId);  // Pass the teacher ID to the detail activity
            teacherDetailLauncher.launch(intent);
        });

        teacherListContainer.addView(teacherCard);
    }
}
