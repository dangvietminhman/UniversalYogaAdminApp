package com.example.universalyogaadminapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EditClassActivity extends AppCompatActivity {

    private EditText editTextDay, editTextCapacity, editTextDuration, editTextPrice, editTextDescription, editTextTime;
    private AutoCompleteTextView autoCompleteTeacher;
    private Spinner spinnerTypeOfClass, spinnerDifficulty;
    private Button buttonSave;
    private ImageView buttonSelectDate, buttonPickTime, buttonBack;
    private YogaClassDatabaseHelper dbHelper;
    private int classId = -1;

    private ArrayList<String> teacherList;
    private ArrayAdapter<String> teacherAdapter;
    private DatabaseReference teacherRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_class);

        Log.d("EditClassActivity", "onCreate called.");

        dbHelper = new YogaClassDatabaseHelper(this);
        initializeViews();
        loadTeacherNamesFromFirebase();

        // Lấy classId từ Intent
        Intent intent = getIntent();
        classId = intent.getIntExtra("classId", -1);

        // Kiểm tra nếu classId không hợp lệ
        if (classId == -1) {
            Toast.makeText(this, "Invalid class ID received", Toast.LENGTH_SHORT).show();
            Log.e("EditClassActivity", "Received invalid classId: " + classId);
            return; // Không gọi finish() để giữ Activity mở
        }

        Log.d("EditClassActivity", "Editing class with ID: " + classId);

        // Tải chi tiết lớp học nếu classId hợp lệ
        loadClassDetails(classId);

        buttonPickTime.setOnClickListener(v -> showTimePickerDialog());
        buttonSelectDate.setOnClickListener(v -> showDatePickerDialog());

        buttonSave.setOnClickListener(v -> {
            new AlertDialog.Builder(EditClassActivity.this)
                    .setTitle("Confirm Edit")
                    .setMessage("Are you sure you want to save these changes?")
                    .setPositiveButton("Confirm", (dialog, which) -> saveClassDetails())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        buttonBack.setOnClickListener(v -> finish());

        addTextWatcherForDate();
        addTextWatcherForTime();

        // Add TextWatcher for dynamic filtering of teacher suggestions
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
        autoCompleteTeacher = findViewById(R.id.autoCompleteTeacher);

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
        buttonSave = findViewById(R.id.buttonUpdate);
        buttonBack = findViewById(R.id.buttonBack);

        teacherList = new ArrayList<>();
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teacherList);
        autoCompleteTeacher.setAdapter(teacherAdapter);
        autoCompleteTeacher.setThreshold(1);

        // Display the dropdown with all teachers when clicking or focusing on the field
        autoCompleteTeacher.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) autoCompleteTeacher.showDropDown();
        });
        autoCompleteTeacher.setOnClickListener(v -> autoCompleteTeacher.showDropDown());
    }

    private void loadTeacherNamesFromFirebase() {
        teacherRef = FirebaseDatabase.getInstance().getReference("teachers");

        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                teacherList.clear();
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String teacherName = teacherSnapshot.child("name").getValue(String.class);
                    if (teacherName != null) {
                        teacherList.add(teacherName);
                    }
                }
                teacherAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditClassActivity.this, "Failed to load teachers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTeacherSuggestions(String query) {
        if (query.isEmpty()) {
            autoCompleteTeacher.dismissDropDown();
            return;
        }

        ArrayList<String> filteredList = new ArrayList<>();
        for (String teacher : teacherList) {
            if (teacher.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(teacher);
            }
        }

        if (filteredList.isEmpty()) {
            filteredList.add("No matching teacher found");
        }

        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredList);
        autoCompleteTeacher.setAdapter(teacherAdapter);
        autoCompleteTeacher.showDropDown();
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

    private void loadClassDetails(int classId) {
        Cursor cursor = dbHelper.getClassById(classId);
        if (cursor != null && cursor.moveToFirst()) {
            String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
            int capacity = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_CAPACITY));
            String duration = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DURATION));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_PRICE));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));
            String difficulty = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DIFFICULTY));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DESCRIPTION));

            editTextDay.setText(day);
            editTextTime.setText(time);
            editTextCapacity.setText(String.valueOf(capacity));
            editTextDuration.setText(duration);
            editTextPrice.setText(String.valueOf(price));
            autoCompleteTeacher.setText(teacher);
            editTextDescription.setText(description);

            spinnerTypeOfClass.setSelection(getSpinnerIndex(spinnerTypeOfClass, type));
            spinnerDifficulty.setSelection(getSpinnerIndex(spinnerDifficulty, difficulty));

            cursor.close();
        } else {
            // Không gọi finish(), chỉ hiển thị thông báo nếu không tìm thấy classId
            Toast.makeText(this, "Class details not found", Toast.LENGTH_SHORT).show();
            Log.e("EditClassActivity", "No details found for classId: " + classId);
        }
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private void saveClassDetails() {
        String day = editTextDay.getText().toString().trim();
        String time = editTextTime.getText().toString().trim();
        String duration = editTextDuration.getText().toString().trim();
        String teacher = autoCompleteTeacher.getText().toString().trim();

        if (day.isEmpty() || time.isEmpty() || duration.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues classDetails = new ContentValues();
        classDetails.put(YogaClassDatabaseHelper.COLUMN_DAY, day);
        classDetails.put(YogaClassDatabaseHelper.COLUMN_TIME, time);
        classDetails.put(YogaClassDatabaseHelper.COLUMN_DURATION, duration);
        classDetails.put(YogaClassDatabaseHelper.COLUMN_TEACHER, teacher);

        int result = dbHelper.updateClass(classId, classDetails);
        if (result > 0) {
            Toast.makeText(this, "Class details updated successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK); // Chỉ gọi setResult và finish khi cập nhật thành công
            finish();
        } else {
            Toast.makeText(this, "Failed to update class details", Toast.LENGTH_SHORT).show();
        }
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
}
