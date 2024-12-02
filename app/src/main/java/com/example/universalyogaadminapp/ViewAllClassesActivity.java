package com.example.universalyogaadminapp;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ViewAllClassesActivity extends AppCompatActivity {

    private LinearLayout classListContainer;
    private YogaClassDatabaseHelper dbHelper;
    private AutoCompleteTextView autoCompleteTeacher;
    private Handler handler = new Handler(Looper.getMainLooper());  // Handler for debounce
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;  // Request code for storage permission


    // Toggle-related variables
    private LinearLayout searchOptionsContainer;
    private ImageView iconExpandCollapse;
    private boolean isSearchOptionsVisible = false;  // Tracks visibility of search options

    // ActivityResultLauncher for course detail
    private final ActivityResultLauncher<Intent> courseDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadClasses();  // Reload classes if the data changed
                    syncAllClassesWithFirebase();  // Sync data after editing
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_classes);

        classListContainer = findViewById(R.id.classListContainer);
        autoCompleteTeacher = findViewById(R.id.autoCompleteTeacher);
        dbHelper = new YogaClassDatabaseHelper(this);

        // Load all classes into the layout
        loadClasses();

        // Search options toggle logic
        searchOptionsContainer = findViewById(R.id.searchOptionsContainer);
        iconExpandCollapse = findViewById(R.id.iconExpandCollapse);

        LinearLayout searchOptionsHeader = findViewById(R.id.searchOptionsHeader);
        searchOptionsHeader.setOnClickListener(v -> toggleSearchOptions());

        // Search Button for category
        Button buttonSearchCategory = findViewById(R.id.buttonSearchCategory);
        buttonSearchCategory.setOnClickListener(v -> showCategoryDialog());

        // Search Button for teacher
        Button buttonSearchTeacher = findViewById(R.id.buttonSearchTeacher);
        buttonSearchTeacher.setOnClickListener(v -> {
            autoCompleteTeacher.setVisibility(autoCompleteTeacher.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (autoCompleteTeacher.getVisibility() == View.VISIBLE) {
                loadTeacherSuggestions();  // Load teacher suggestions when visible
            }
        });

        // Search Button for date
        Button buttonSearchByDate = findViewById(R.id.buttonSearchByDate);
        buttonSearchByDate.setOnClickListener(v -> showDatePickerDialog());

        // Back button functionality
        ImageView backIcon = findViewById(R.id.backIcon);
        backIcon.setOnClickListener(v -> finish());

        // Handle real-time suggestions and teacher search with debounce
        autoCompleteTeacher.addTextChangedListener(new android.text.TextWatcher() {
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    if (!s.toString().isEmpty()) {
                        loadTeacherSuggestions(s.toString());  // Fetch suggestions based on input
                    }
                };
                handler.postDelayed(searchRunnable, 300);  // Delay 300ms before searching
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        autoCompleteTeacher.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTeacher = (String) parent.getItemAtPosition(position);
            loadClassesByTeacher(selectedTeacher);  // Load classes by selected teacher
        });

        // Button to export to Excel
        Button buttonExportToExcel = findViewById(R.id.buttonExportToExcel);
        buttonExportToExcel.setOnClickListener(v -> checkAndRequestPermissions());  // Call permission checking method
    }

    // Check and request permission to write to external storage
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {  // Permission needed only for Android 9 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            } else {
                exportDataToExcel();  // If permission already granted, export the data
            }
        } else {
            exportDataToExcel();  // For Android 10 and above, no permission required
        }
    }


    // Handle the result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDataToExcel();
            } else {
                Toast.makeText(this, "Permission denied to write to external storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to export class data to Excel file
    private void exportDataToExcel() {
        Cursor cursor = dbHelper.getAllClasses();  // Lấy tất cả các lớp từ cơ sở dữ liệu

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No classes found to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo workbook và sheet mới
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Yoga Classes");

        // Tạo dòng tiêu đề
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Class ID");
        headerRow.createCell(1).setCellValue("Day");
        headerRow.createCell(2).setCellValue("Time");
        headerRow.createCell(3).setCellValue("Type");
        headerRow.createCell(4).setCellValue("Teacher");
        headerRow.createCell(5).setCellValue("Capacity");
        headerRow.createCell(6).setCellValue("Duration");
        headerRow.createCell(7).setCellValue("Price");

        // Điền dữ liệu vào các dòng
        int rowIndex = 1;
        while (cursor.moveToNext()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID)));
            row.createCell(1).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY)));
            row.createCell(2).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME)));
            row.createCell(3).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE)));
            row.createCell(4).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER)));
            row.createCell(5).setCellValue(cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_CAPACITY)));
            row.createCell(6).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DURATION)));
            row.createCell(7).setCellValue(cursor.getDouble(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_PRICE)));
        }

        cursor.close();  // Đóng cursor sau khi xong

        // Lưu workbook dưới dạng file Excel
        String fileName = "YogaClasses.xlsx";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
            workbook.close();
            Toast.makeText(this, "Excel file saved to Downloads", Toast.LENGTH_SHORT).show();

            // Tự động mở file Excel
            openExcelFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating Excel file", Toast.LENGTH_SHORT).show();
        }
    }


    // Method to open the Excel file using an Intent
    private void openExcelFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(androidx.core.content.FileProvider.getUriForFile(this, "com.example.universalyogaadminapp.provider", file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Kiểm tra nếu có ứng dụng nào có thể mở file Excel
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);  // Mở file Excel
        } else {
            Toast.makeText(this, "No app available to open Excel file", Toast.LENGTH_SHORT).show();
        }
    }


    // Toggle search options visibility
    private void toggleSearchOptions() {
        if (isSearchOptionsVisible) {
            searchOptionsContainer.setVisibility(View.GONE);
            iconExpandCollapse.setImageResource(R.drawable.ic_expand_more);  // Update icon to 'expand'
        } else {
            searchOptionsContainer.setVisibility(View.VISIBLE);
            iconExpandCollapse.setImageResource(R.drawable.ic_expand_less);  // Update icon to 'collapse'
        }
        isSearchOptionsVisible = !isSearchOptionsVisible;
    }

    // Display the category dialog
    private void showCategoryDialog() {
        String[] categories = {"Flow Yoga", "Aerial Yoga", "Family Yoga"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Class Type")
                .setItems(categories, (dialog, which) -> {
                    String selectedType = categories[which];
                    loadClassesByType(selectedType);
                });
        builder.create().show();
    }

    // Method to show the date picker dialog
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
            loadClassesByDate(selectedDate);  // Load classes by the selected date
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    // Load teacher suggestions into AutoCompleteTextView using Cursor
    private void loadTeacherSuggestions() {
        Cursor cursor = dbHelper.getAllTeacherNamesCursor();  // Get all teacher names as a Cursor from the database

        ArrayList<String> teacherNames = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME)); // Use teacher_name instead of teacher
                teacherNames.add(teacher);
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teacherNames);
        autoCompleteTeacher.setAdapter(adapter);
    }

    // Method to load teacher suggestions dynamically based on input using Cursor
    private void loadTeacherSuggestions(String input) {
        Cursor cursor = dbHelper.getTeacherNamesLike(input);  // Search for teachers matching the input using Cursor

        ArrayList<String> teacherNames = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER_NAME)); // Correct column name
                teacherNames.add(teacher);
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teacherNames);
        autoCompleteTeacher.setAdapter(adapter);
        autoCompleteTeacher.showDropDown();  // Show suggestions
    }

    // Method to load classes by teacher name
    private void loadClassesByTeacher(String teacherName) {
        Cursor cursor = dbHelper.getClassesByTeacher(teacherName);

        TextView searchResultMessage = findViewById(R.id.textViewSearchResult);  // Get the result message view

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No classes found for teacher " + teacherName, Toast.LENGTH_SHORT).show();
            classListContainer.removeAllViews();
            searchResultMessage.setVisibility(View.GONE);  // Hide the message if no results are found
            return;
        }

        classListContainer.removeAllViews();
        searchResultMessage.setText(cursor.getCount() + " classes found for teacher " + teacherName);
        searchResultMessage.setVisibility(View.VISIBLE);  // Show the success message

        while (cursor.moveToNext()) {
            int classId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID));
            String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));

            addClassCard(classId, day, time, teacher, type);
        }

        cursor.close();
    }

    // Method to load classes by type
    private void loadClassesByType(String type) {
        Cursor cursor = dbHelper.getClassesByType(type);

        TextView searchResultMessage = findViewById(R.id.textViewSearchResult);  // Get the result message view

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No classes found for " + type, Toast.LENGTH_SHORT).show();
            classListContainer.removeAllViews();
            searchResultMessage.setVisibility(View.GONE);  // Hide the message if no results are found
            return;
        }

        classListContainer.removeAllViews();
        searchResultMessage.setText(cursor.getCount() + " classes found for " + type);
        searchResultMessage.setVisibility(View.VISIBLE);  // Show the success message

        while (cursor.moveToNext()) {
            int classId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID));
            String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));

            addClassCard(classId, day, time, teacher, type);
        }

        cursor.close();
    }

    // Method to load classes by date
    private void loadClassesByDate(String date) {
        Cursor cursor = dbHelper.getClassesByDate(date);

        TextView searchResultMessage = findViewById(R.id.textViewSearchResult);  // Get the result message view

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No classes found for " + date, Toast.LENGTH_SHORT).show();
            classListContainer.removeAllViews();
            searchResultMessage.setVisibility(View.GONE);  // Hide the message if no results are found
            return;
        }

        classListContainer.removeAllViews();
        searchResultMessage.setText(cursor.getCount() + " classes found for " + date);
        searchResultMessage.setVisibility(View.VISIBLE);  // Show the success message

        while (cursor.moveToNext()) {
            int classId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID));
            String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));

            addClassCard(classId, day, time, teacher, type);
        }

        cursor.close();
    }

    // Modified loadClasses method with Firebase sync
    private void loadClasses() {
        Cursor cursor = dbHelper.getAllClasses();

        if (cursor.getCount() == 0) {
            // No classes found locally, sync from Firebase
            syncClassesFromFirebase();
            return;
        }

        classListContainer.removeAllViews();

        while (cursor.moveToNext()) {
            int classId = cursor.getInt(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_ID));
            String day = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_DAY));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TIME));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TYPE));
            String teacher = cursor.getString(cursor.getColumnIndexOrThrow(YogaClassDatabaseHelper.COLUMN_TEACHER));

            addClassCard(classId, day, time, teacher, type);
        }

        cursor.close();
    }

    private void syncClassesFromFirebase() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference classesRef = firebaseDatabase.getReference("classes");

        classesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot classSnapshot : task.getResult().getChildren()) {
                    // Fetch each class and insert it into the local database
                    String day = classSnapshot.child("day").getValue(String.class);
                    String time = classSnapshot.child("time").getValue(String.class);
                    int capacity = classSnapshot.child("capacity").getValue(Integer.class);
                    String duration = classSnapshot.child("duration").getValue(String.class);
                    double price = classSnapshot.child("price").getValue(Double.class);
                    String type = classSnapshot.child("type").getValue(String.class);
                    String teacher = classSnapshot.child("teacher").getValue(String.class);
                    String description = classSnapshot.child("description").getValue(String.class);

                    // Create ContentValues to insert data into SQLite
                    ContentValues values = new ContentValues();
                    values.put(YogaClassDatabaseHelper.COLUMN_DAY, day);
                    values.put(YogaClassDatabaseHelper.COLUMN_TIME, time);
                    values.put(YogaClassDatabaseHelper.COLUMN_CAPACITY, capacity);
                    values.put(YogaClassDatabaseHelper.COLUMN_DURATION, duration);
                    values.put(YogaClassDatabaseHelper.COLUMN_PRICE, price);
                    values.put(YogaClassDatabaseHelper.COLUMN_TYPE, type);
                    values.put(YogaClassDatabaseHelper.COLUMN_TEACHER, teacher);
                    values.put(YogaClassDatabaseHelper.COLUMN_DESCRIPTION, description);

                    dbHelper.addClass(values);  // Add class to local SQLite database
                }
                loadClasses();  // Reload the classes from the local database after syncing
            } else {
                Toast.makeText(this, "No data found in Firebase or sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to add a class card to the layout
    private void addClassCard(int classId, String day, String time, String teacher, String type) {
        // Create a LinearLayout to act as a card
        LinearLayout classCard = new LinearLayout(this);
        classCard.setOrientation(LinearLayout.VERTICAL);
        classCard.setPadding(24, 24, 24, 24);
        classCard.setBackgroundResource(R.drawable.class_card_background);
        classCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) classCard.getLayoutParams();
        params.setMargins(0, 0, 0, 24);
        classCard.setLayoutParams(params);

        // Add TextViews for class details
        TextView classDay = new TextView(this);
        classDay.setText("Day: " + day);
        classDay.setTextSize(18);
        classDay.setTextColor(Color.BLACK);
        classCard.addView(classDay);

        TextView classTime = new TextView(this);
        classTime.setText("Time: " + time);
        classTime.setTextSize(18);
        classTime.setTextColor(Color.BLACK);
        classCard.addView(classTime);

        TextView classTeacher = new TextView(this);
        classTeacher.setText("Teacher: " + teacher);
        classTeacher.setTextSize(18);
        classTeacher.setTextColor(Color.BLACK);
        classCard.addView(classTeacher);

        TextView classType = new TextView(this);
        classType.setText("Type: " + type); // Add class type
        classType.setTextSize(18);
        classType.setTextColor(Color.BLACK);
        classCard.addView(classType);

        // Handle click event for the card
        classCard.setOnClickListener(view -> {
            Intent intent = new Intent(ViewAllClassesActivity.this, CourseDetailActivity.class);
            intent.putExtra("classId", classId);
            courseDetailLauncher.launch(intent);
        });

        // Add the card to the container
        classListContainer.addView(classCard);
    }


    // Method to sync all classes with Firebase after edit or delete
    private void syncAllClassesWithFirebase() {
        if (!isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference classesRef = firebaseDatabase.getReference("classes");

        Cursor cursor = dbHelper.getAllClasses();

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No classes available for sync", Toast.LENGTH_SHORT).show();
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


        }

        cursor.close();
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
            return activeNetwork != null && (
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
}
