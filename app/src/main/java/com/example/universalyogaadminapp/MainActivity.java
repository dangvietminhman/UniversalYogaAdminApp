package com.example.universalyogaadminapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference to buttons
        Button buttonCreateClass = findViewById(R.id.buttonCreateClass);
        Button buttonViewClasses = findViewById(R.id.buttonViewAllClasses);
        Button buttonAddTeacher = findViewById(R.id.buttonAddTeacher);
        Button buttonViewAllTeachers = findViewById(R.id.buttonViewAllTeachers);
        Button buttonViewBookedClasses = findViewById(R.id.buttonViewBookedClasses); // New button for viewing booked classes

        // Set up the "Create Class" button click event
        buttonCreateClass.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddClassActivity.class);
            startActivity(intent);
        });

        // Set up the "View All Classes" button click event
        buttonViewClasses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewAllClassesActivity.class);
            startActivity(intent);
        });

        // Set up the "Add Teacher" button click event
        buttonAddTeacher.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTeacherActivity.class);
            startActivity(intent);
        });

        // Set up the "View All Teachers" button click event
        buttonViewAllTeachers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewAllTeachersActivity.class);
            startActivity(intent);
        });

        // Set up the "View Booked Classes by Email" button click event
        buttonViewBookedClasses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewBookedClassesActivity.class); // Assume this is your target activity
            startActivity(intent);
        });
    }
}
