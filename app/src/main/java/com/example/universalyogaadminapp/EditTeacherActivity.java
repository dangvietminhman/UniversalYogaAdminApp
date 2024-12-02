package com.example.universalyogaadminapp;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class EditTeacherActivity extends AppCompatActivity {

    private EditText editTextName, editTextAddress, editTextPhone, editTextDescription, editTextAge;
    private Spinner spinnerQualification;
    private Button buttonSave;
    private ImageView buttonBack;
    private YogaClassDatabaseHelper dbHelper;
    private int teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_teacher);

        dbHelper = new YogaClassDatabaseHelper(this);
        initializeViews();

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        teacherId = intent.getIntExtra("teacherId", -1);

        // Load teacher details and update spinner selection once adapter is set
        loadTeacherDetails(teacherId);

        buttonSave.setOnClickListener(v -> confirmSaveTeacherDetails());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextAge = findViewById(R.id.editTextAge);

        spinnerQualification = findViewById(R.id.spinnerQualification);
        buttonSave = findViewById(R.id.buttonUpdate);
        buttonBack = findViewById(R.id.buttonBack);

        // Set adapter for the spinner
        ArrayAdapter<String> qualificationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getQualifications());
        qualificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQualification.setAdapter(qualificationAdapter);
    }

    private ArrayList<String> getQualifications() {
        ArrayList<String> qualificationOptions = new ArrayList<>();
        qualificationOptions.add("Select qualification");
        qualificationOptions.add("Bachelor's Degree");
        qualificationOptions.add("Master's Degree");
        qualificationOptions.add("PhD");
        return qualificationOptions;
    }

    private void loadTeacherDetails(int teacherId) {
        Cursor cursor = dbHelper.getTeacherById(teacherId);
        if (cursor != null && cursor.moveToFirst()) {
            editTextName.setText(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME)));
            editTextAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_ADDRESS)));
            editTextPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_PHONE)));
            editTextDescription.setText(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_DESCRIPTION)));
            editTextAge.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_AGE))));

            // Retrieve the qualification from the database
            String teacherQualification = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION));

            // Wait until spinner adapter is fully initialized, then set spinner value
            spinnerQualification.post(() -> setSpinnerValue(spinnerQualification, teacherQualification));

            cursor.close();
        }
    }

    // Helper method to set the spinner value based on the qualification string
    private void setSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().trim().equalsIgnoreCase(value.trim())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void confirmSaveTeacherDetails() {
        // Hiển thị hộp thoại xác nhận trước khi lưu
        new AlertDialog.Builder(this)
                .setTitle("Confirm Save")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes", (dialog, which) -> saveTeacherDetails())
                .setNegativeButton("No", null)
                .show();
    }

    private void saveTeacherDetails() {
        String name = editTextName.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String qualification = spinnerQualification.getSelectedItem().toString();
        int age;

        // Validate the name, address, phone, and age fields
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editTextAge.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter an age", Toast.LENGTH_SHORT).show();
            return;
        } else {
            age = Integer.parseInt(editTextAge.getText().toString().trim());
        }

        // Validate the qualification, ensuring that the user selects a valid option
        if (qualification.equals("Select qualification")) {
            Toast.makeText(this, "Please select a valid qualification", Toast.LENGTH_SHORT).show();
            return;
        }

        // If all validations pass, proceed to save the teacher details
        ContentValues teacherDetails = new ContentValues();
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME, name);
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_ADDRESS, address);
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_PHONE, phone);
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_AGE, age);
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION, qualification);
        teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_DESCRIPTION, description);

        int result = dbHelper.updateTeacher(teacherId, teacherDetails);
        if (result > 0) {
            Toast.makeText(this, "Teacher details updated successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();  // Quay lại TeacherDetailActivity
        } else {
            Toast.makeText(this, "Failed to update teacher details", Toast.LENGTH_SHORT).show();
        }
    }
}
