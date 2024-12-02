    package com.example.universalyogaadminapp;

    import android.content.Intent;
    import android.database.Cursor;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.TextView;
    import android.widget.Toast;
    import android.util.Log;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;

    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;

    public class CourseDetailActivity extends AppCompatActivity {

        private TextView textViewClassDetails, textViewBookingEmail;
        private Button buttonEdit, buttonDelete;
        private ImageView backButton;
        private YogaClassDatabaseHelper dbHelper;
        private int classId;
        private DatabaseReference ordersRef; // Firebase database reference
        private String orderId;  // Use orderId to identify the correct booking in Firebase

        // ActivityResultLauncher to get result from EditClassActivity
        private ActivityResultLauncher<Intent> editClassLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Refresh data when returning from EditClassActivity
                        displayClassDetails(classId);
                    }
                }
        );

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_course_detail);

            // Bind views
            textViewClassDetails = findViewById(R.id.textViewClassDetails);
            buttonEdit = findViewById(R.id.buttonEdit);
            buttonDelete = findViewById(R.id.buttonDelete);
            backButton = findViewById(R.id.backButton);

            dbHelper = new YogaClassDatabaseHelper(this);

            // Get classId from Intent
            Intent intent = getIntent();
            classId = intent.getIntExtra("classId", -1);

            // Display class details and booking status
            displayClassDetails(classId);

            // Handle Edit button click
            buttonEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(CourseDetailActivity.this, EditClassActivity.class);
                editIntent.putExtra("classId", classId);
                editClassLauncher.launch(editIntent);
            });

            // Handle Delete button click
            buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

            // Handle Back button click
            backButton.setOnClickListener(v -> onBackPressed());
        }

        @Override
        public void onBackPressed() {
            // Set result before returning to ViewAllClassesActivity
            setResult(RESULT_OK);
            super.onBackPressed();
        }

        // Method to display class details from database
        private void displayClassDetails(int classId) {
            Cursor cursor = dbHelper.getClassById(classId);  // Query class from database
            if (cursor != null && cursor.moveToFirst()) {
                // Get class details
                String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
                int capacity = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_CAPACITY));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DURATION));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_PRICE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));
                String difficulty = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DIFFICULTY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DESCRIPTION));

                // Display class details on TextView
                String classDetails = String.format("Day: %s\nTime: %s\nCapacity: %d\nDuration: %s\nPrice: %.2f\nType: %s\nTeacher: %s\nDifficulty: %s\nDescription: %s",
                        day, time, capacity, duration, price, type, teacher, difficulty, description);
                textViewClassDetails.setText(classDetails);

                // Close cursor after use
                cursor.close();
            } else {
                Toast.makeText(this, "Failed to load class details", Toast.LENGTH_SHORT).show();
            }
        }

        // Show confirmation dialog for deletion
        private void showDeleteConfirmationDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Class")
                    .setMessage("Are you sure you want to delete this class?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete class from database
                        int rowsAffected = dbHelper.deleteClass(classId);
                        if (rowsAffected > 0) {
                            Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();  // Return to previous screen
                        } else {
                            Toast.makeText(this, "Failed to delete class", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
