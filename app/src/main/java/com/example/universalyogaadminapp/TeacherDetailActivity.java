package com.example.universalyogaadminapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TeacherDetailActivity extends AppCompatActivity {

    private TextView textViewTeacherDetails;
    private Button buttonEdit, buttonDelete;
    private ImageView backButton;
    private YogaClassDatabaseHelper dbHelper;
    private int teacherId;

    // Launcher to receive result from EditTeacherActivity
    private final ActivityResultLauncher<Intent> editTeacherLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Refresh teacher details if edited
                    displayTeacherDetails(teacherId);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_detail);

        textViewTeacherDetails = findViewById(R.id.textViewTeacherDetails);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        backButton = findViewById(R.id.backButton);

        dbHelper = new YogaClassDatabaseHelper(this);

        // Get teacherId from intent
        Intent intent = getIntent();
        teacherId = intent.getIntExtra("teacherId", -1);

        // Display teacher details from database
        displayTeacherDetails(teacherId);

        // TeacherDetailActivity
        buttonEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(TeacherDetailActivity.this, EditTeacherActivity.class);
            editIntent.putExtra("teacherId", teacherId);  // Truyền teacherId
            editTeacherLauncher.launch(editIntent);
        });


        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void displayTeacherDetails(int teacherId) {
        Cursor cursor = dbHelper.getTeacherById(teacherId);
        if (cursor != null && cursor.moveToFirst()) {
            // Fetching details from the cursor
            String name = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME));
            int age = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_AGE));
            String address = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_ADDRESS));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_PHONE));
            String qualification = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_QUALIFICATION));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_DESCRIPTION));

            // Formatting details for display
            String teacherDetails = String.format("Name: %s\nAge: %d\nAddress: %s\nPhone: %s\nQualification: %s\nDescription: %s",
                    name, age, address, phone, qualification, description);
            textViewTeacherDetails.setText(teacherDetails);

            cursor.close();
        } else {
            Toast.makeText(this, "Failed to load teacher details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Teacher")
                .setMessage("Are you sure you want to delete this teacher?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", (dialog, which) -> {
                    int rowsAffected = dbHelper.deleteTeacher(teacherId);
                    if (rowsAffected > 0) {
                        Toast.makeText(this, "Teacher deleted", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete teacher", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
