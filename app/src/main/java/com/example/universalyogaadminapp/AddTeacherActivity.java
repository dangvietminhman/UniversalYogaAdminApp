package com.example.universalyogaadminapp;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;

import java.util.Arrays;
import java.util.List;

public class AddTeacherActivity extends AppCompatActivity {

    private EditText editTextName, editTextAddress, editTextPhone, editTextAge, editTextDescription;
    private Spinner spinnerQualification;
    private Button buttonSubmit;
    private YogaClassDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_teacher);

        dbHelper = new YogaClassDatabaseHelper(this);
        initializeViews();

        // Populate Spinner with qualifications (list directly in code)
        List<String> qualifications = Arrays.asList("Select Qualification", "Bachelor's Degree", "Master's Degree", "PhD");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, qualifications);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQualification.setAdapter(adapter);

        buttonSubmit.setOnClickListener(v -> submitTeacherDetails());

        // Handle the back button click (ImageView)
        ImageView buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            // Navigate back to MainActivity by finishing the current activity
            finish();
        });
    }

    private void initializeViews() {
        editTextName = findViewById(R.id.editTextTeacherName);
        editTextAddress = findViewById(R.id.editTextTeacherAddress);
        editTextPhone = findViewById(R.id.editTextTeacherPhone);
        editTextAge = findViewById(R.id.editTextTeacherAge);
        editTextDescription = findViewById(R.id.editTextTeacherDescription);
        spinnerQualification = findViewById(R.id.spinnerQualification);
        buttonSubmit = findViewById(R.id.buttonSubmitTeacher);
    }

    private void submitTeacherDetails() {
        String name = editTextName.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String ageStr = editTextAge.getText().toString().trim();
        String qualification = spinnerQualification.getSelectedItem().toString();
        String description = editTextDescription.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty() || phone.isEmpty() || ageStr.isEmpty() || qualification.equals("Select Qualification")) {
            Toast.makeText(this, "Please fill all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid age!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo hộp thoại xác nhận
        new AlertDialog.Builder(this)
                .setTitle("Confirm Submission")
                .setMessage("Are you sure you want to add this teacher?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Nếu người dùng xác nhận, thêm giáo viên vào cơ sở dữ liệu
                    ContentValues teacherDetails = new ContentValues();
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME, name);
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_ADDRESS, address);
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_PHONE, phone);
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_AGE, age);
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION, qualification);
                    teacherDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER_DESCRIPTION, description);

                    long result = dbHelper.addTeacher(teacherDetails);
                    if (result != -1) {
                        Toast.makeText(this, "Teacher added successfully!", Toast.LENGTH_SHORT).show();
                        clearFields();
                    } else {
                        Toast.makeText(this, "Failed to add teacher!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Nếu người dùng từ chối, đóng hộp thoại
                    dialog.dismiss();
                })
                .show();
    }


    private void clearFields() {
        editTextName.setText("");
        editTextAddress.setText("");
        editTextPhone.setText("");
        editTextAge.setText("");
        editTextDescription.setText("");
        spinnerQualification.setSelection(0);
    }
}
