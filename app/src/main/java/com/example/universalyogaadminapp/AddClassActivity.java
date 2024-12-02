package com.example.universalyogaadminapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AddClassActivity extends AppCompatActivity {

    private EditText editTextDay, editTextCapacity, editTextDuration, editTextPrice, editTextDescription, editTextTime;
    private AutoCompleteTextView autoCompleteTeacher;
    private Spinner spinnerTypeOfClass, spinnerDifficulty;
    private Button buttonSubmit;
    private ImageView buttonSelectDate, buttonPickTime, buttonBack;
    private YogaClassDatabaseHelper dbHelper;
    private ArrayAdapter<String> teacherAdapter;
    private ArrayList<String> teacherList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);

        dbHelper = new YogaClassDatabaseHelper(this);
        initializeViews();
        loadTeachers(); // Load teacher list from the database

        buttonPickTime.setOnClickListener(v -> showTimePickerDialog());
        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog());
        buttonSubmit.setOnClickListener(v -> submitClassDetails());

        buttonBack.setOnClickListener(v -> finish());

        addTextWatcherForDate();
        addTextWatcherForTime();

        // Add TextWatcher to dynamically filter teachers as user types
        autoCompleteTeacher.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTeacherSuggestions(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeViews() {
        editTextDay = findViewById(R.id.editTextDay);
        editTextCapacity = findViewById(R.id.editTextCapacity);
        editTextDuration = findViewById(R.id.editTextDuration);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextTime = findViewById(R.id.editTextTime);
        autoCompleteTeacher = findViewById(R.id.autoCompleteTeacher); // AutoCompleteTextView for teachers

        spinnerTypeOfClass = findViewById(R.id.spinnerTypeOfClass);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getClassTypes());
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(typeAdapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getDifficultyLevels());
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        buttonPickTime = findViewById(R.id.buttonPickTime);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonBack = findViewById(R.id.buttonBack);
    }

    // Load all teachers initially into AutoCompleteTextView
    private void loadTeachers() {
        teacherList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllTeacherNamesCursor();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                teacherList.add(cursor.getString(0)); // Add teacher name to the list
            }
            cursor.close();
        }

        if (teacherList.isEmpty()) {
            Toast.makeText(this, "No teachers found in the database", Toast.LENGTH_SHORT).show();
            Log.d("TeacherLoad", "No teachers found in the database.");
        } else {
            Log.d("TeacherLoad", "Teachers loaded: " + teacherList.size());

            teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teacherList);
            autoCompleteTeacher.setAdapter(teacherAdapter);
            autoCompleteTeacher.setThreshold(1); // Show suggestions after typing 1 character

            // Automatically show all teachers when the user focuses or clicks on the field
            autoCompleteTeacher.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    autoCompleteTeacher.showDropDown(); // Show dropdown when focused
                }
            });

            // Also show the dropdown when the user clicks inside the field
            autoCompleteTeacher.setOnClickListener(v -> {
                autoCompleteTeacher.showDropDown(); // Show dropdown when clicked
            });
        }
    }


    // Method to filter teacher suggestions based on input
    private void filterTeacherSuggestions(String query) {
        if (query.isEmpty()) {
            // If the input is empty, hide the dropdown and return
            autoCompleteTeacher.dismissDropDown(); // Hide dropdown if input is cleared
            return;
        }

        ArrayList<String> filteredList = new ArrayList<>();

        // Query database for matching teachers if input is not empty
        Cursor cursor = dbHelper.getTeacherNamesLike(query);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                filteredList.add(cursor.getString(0)); // Add matched teacher name to the filtered list
            }
            cursor.close();
        }

        if (filteredList.isEmpty()) {
            // Add a single "No matching teacher found" item if the list is empty
            filteredList.add("No matching teacher found");
        }

        // Update the adapter with the filtered list
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredList);
        autoCompleteTeacher.setAdapter(teacherAdapter);
        autoCompleteTeacher.showDropDown(); // Show dropdown suggestions
    }



    private ArrayList<String> getClassTypes() {
        ArrayList<String> classOptions = new ArrayList<>();
        classOptions.add("Select class type");
        classOptions.add("Flow Yoga");
        classOptions.add("Aerial Yoga");
        classOptions.add("Family Yoga");
        return classOptions;
    }

    private ArrayList<String> getDifficultyLevels() {
        ArrayList<String> difficultyOptions = new ArrayList<>();
        difficultyOptions.add("Select difficulty level");
        difficultyOptions.add("Easy");
        difficultyOptions.add("Medium");
        difficultyOptions.add("Hard");
        return difficultyOptions;
    }

    private void submitClassDetails() {
        try {
            String day = editTextDay.getText().toString().trim();
            String time = editTextTime.getText().toString().trim();
            String capacityStr = editTextCapacity.getText().toString().trim();
            String duration = editTextDuration.getText().toString().trim();
            String priceStr = editTextPrice.getText().toString().trim();
            String type = spinnerTypeOfClass.getSelectedItem().toString().trim();
            String teacher = autoCompleteTeacher.getText().toString().trim();
            String difficulty = spinnerDifficulty.getSelectedItem().toString().trim();
            String description = editTextDescription.getText().toString().trim();

            // Check each field and show a specific message if it is empty or invalid
            if (day.isEmpty()) {
                Toast.makeText(this, "Please enter the day.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (time.isEmpty()) {
                Toast.makeText(this, "Please enter the time.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (capacityStr.isEmpty()) {
                Toast.makeText(this, "Please enter the capacity.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (duration.isEmpty()) {
                Toast.makeText(this, "Please enter the duration.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Please enter the price.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (type.equals("Select class type")) {
                Toast.makeText(this, "Please select a class type.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (difficulty.equals("Select difficulty level")) {
                Toast.makeText(this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (teacher.isEmpty()) {
                Toast.makeText(this, "Please select or enter a teacher.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!teacherList.contains(teacher)) {
                Toast.makeText(this, "Please select a valid teacher from the list.", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity = Integer.parseInt(capacityStr);
            double price = Double.parseDouble(priceStr);

            // Show confirmation dialog
            showConfirmationDialog(day, time, capacity, duration, price, type, teacher, difficulty, description);

        } catch (NumberFormatException e) {
            Log.e("AddClassActivity", "Error parsing numeric input", e);
            Toast.makeText(this, "Please enter valid numeric values for capacity and price.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AddClassActivity", "Error creating class", e);
            Toast.makeText(this, "An error occurred while creating the class.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showConfirmationDialog(String day, String time, int capacity, String duration, double price,
                                        String type, String teacher, String difficulty, String description) {
        String message = String.format(Locale.getDefault(),
                "Day: %s\nTime: %s\nCapacity: %d\nDuration: %s\nPrice: %.2f\nType: %s\nTeacher: %s\nDifficulty: %s\nDescription: %s",
                day, time, capacity, duration, price, type, teacher, difficulty, description);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Class Details")
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Save class details to database
                    ContentValues classDetails = new ContentValues();
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_DAY, day);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_TIME, time);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_CAPACITY, capacity);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_DURATION, duration);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_PRICE, price);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_TYPE, type);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER, teacher);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_DIFFICULTY, difficulty);
                    classDetails.put(YogaClassDatabaseHelper.COLUMN_DESCRIPTION, description);

                    long result = dbHelper.addClass(classDetails);
                    if (result != -1) {
                        Toast.makeText(this, "Class details saved!", Toast.LENGTH_SHORT).show();
                        uploadAllClassesToFirebase();
                        clearFields();
                        loadTeachers();
                    } else {
                        Toast.makeText(this, "Failed to save class!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Edit", (dialog, which) -> {
                    // Allow user to go back and edit the details
                    dialog.dismiss();
                })
                .show();
    }

    private void clearFields() {
        editTextDay.setText("");
        editTextTime.setText("");
        editTextCapacity.setText("");
        editTextDuration.setText("");
        editTextPrice.setText("");
        autoCompleteTeacher.setText(""); // Clear teacher selection
        editTextDescription.setText("");
        spinnerTypeOfClass.setSelection(0);
        spinnerDifficulty.setSelection(0);
    }

    private void addTextWatcherForDate() {
        editTextDay.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String ddmmyyyy = "DDMMYYYY";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]", "");
                    String cleanC = current.replaceAll("[^\\d.]", "");
                    int sel = clean.length();
                    for (int i = 2; i <= clean.length() && i < 6; i += 2) sel++;
                    if (clean.length() < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        int day = Integer.parseInt(clean.substring(0, 2));
                        int month = Integer.parseInt(clean.substring(2, 4));
                        int year = Integer.parseInt(clean.substring(4, 8));
                        cal.set(Calendar.DAY_OF_MONTH, day);
                        cal.set(Calendar.MONTH, month - 1);
                        cal.set(Calendar.YEAR, year);
                        clean = String.format("%02d%02d%04d", day, month, year);
                    }
                    clean = String.format("%s/%s/%s", clean.substring(0, 2), clean.substring(2, 4), clean.substring(4, 8));
                    current = clean;
                    editTextDay.setText(current);
                    editTextDay.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void addTextWatcherForTime() {
        editTextTime.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String hhmm = "HHMM";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]", "");
                    String cleanC = current.replaceAll("[^\\d.]", "");
                    int sel = clean.length();
                    for (int i = 2; i <= clean.length() && i < 4; i += 2) sel++;
                    if (clean.length() < 4) {
                        clean = clean + hhmm.substring(clean.length());
                    } else {
                        int hour = Integer.parseInt(clean.substring(0, 2));
                        int minute = Integer.parseInt(clean.substring(2, 4));
                        clean = String.format("%02d%02d", hour, minute);
                    }
                    clean = String.format("%s:%s", clean.substring(0, 2), clean.substring(2, 4));
                    current = clean;
                    editTextTime.setText(current);
                    editTextTime.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            editTextTime.setText(formattedTime);
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, dayOfMonth) -> {
            String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (selectedMonth + 1), selectedYear);
            editTextDay.setText(formattedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void uploadAllClassesToFirebase() {
        try {
            if (!isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            DatabaseReference classesRef = firebaseDatabase.getReference("classes");

            Cursor cursor = dbHelper.getAllClasses();

            if (cursor.getCount() == 0) {
                Toast.makeText(this, "No classes available for upload", Toast.LENGTH_SHORT).show();
                return;
            }

            while (cursor.moveToNext()) {
                int classId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID));
                String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
                int capacity = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_CAPACITY));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DURATION));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_PRICE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DESCRIPTION));

                HashMap<String, Object> yogaClass = new HashMap<>();
                yogaClass.put("day", day);
                yogaClass.put("time", time);
                yogaClass.put("capacity", capacity);
                yogaClass.put("duration", duration);
                yogaClass.put("price", price);
                yogaClass.put("type", type);
                yogaClass.put("teacher", teacher);
                yogaClass.put("description", description);

                classesRef.child(String.valueOf(classId)).setValue(yogaClass)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Class uploaded: " + classId, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseUpload", "Failed to upload class", e);
                            Toast.makeText(this, "Failed to upload class: " + classId, Toast.LENGTH_SHORT).show();
                        });
            }

            cursor.close();
        } catch (Exception e) {
            Log.e("UploadClasses", "Error uploading classes", e);
            Toast.makeText(this, "Error occurred during upload", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkInfo network = connectivityManager.getActiveNetworkInfo();
                return network != null && network.isConnected();
            } else {
                // For older devices
                @SuppressWarnings("deprecation")
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }
}
