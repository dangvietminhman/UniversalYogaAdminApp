package com.example.universalyogaadminapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class YogaClassDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Classes.db";

    // Table for classes
    public static final String TABLE_CLASSES = "classes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_CAPACITY = "capacity";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_TEACHER = "teacher";
    public static final String COLUMN_DIFFICULTY = "difficulty"; // Ensure this is included correctly
    public static final String COLUMN_DESCRIPTION = "description";

    // Table for teachers
    public static final String TABLE_TEACHERS = "teachers";
    public static final String COLUMN_TEACHER_ID = "teacher_id";
    public static final String COLUMN_TEACHER_NAME = "teacher_name";
    public static final String COLUMN_TEACHER_ADDRESS = "teacher_address";
    public static final String COLUMN_TEACHER_PHONE = "teacher_phone";
    public static final String COLUMN_TEACHER_AGE = "teacher_age";
    public static final String COLUMN_TEACHER_QUALIFICATION = "teacher_qualification";
    public static final String COLUMN_TEACHER_DESCRIPTION = "teacher_description";

    private Context context;
    private DatabaseReference firebaseRef;

    public YogaClassDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
        this.context = context;
        this.firebaseRef = FirebaseDatabase.getInstance().getReference();  // Firebase root reference
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table for classes
        String CREATE_TABLE_CLASSES = "CREATE TABLE IF NOT EXISTS " + TABLE_CLASSES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DAY + " TEXT,"
                + COLUMN_TIME + " TEXT,"
                + COLUMN_CAPACITY + " INTEGER,"
                + COLUMN_DURATION + " TEXT,"
                + COLUMN_PRICE + " REAL,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_TEACHER + " TEXT,"
                + COLUMN_DIFFICULTY + " TEXT," // Make sure difficulty is properly handled
                + COLUMN_DESCRIPTION + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE_CLASSES);

        // Create table for teachers
        String CREATE_TABLE_TEACHERS = "CREATE TABLE IF NOT EXISTS " + TABLE_TEACHERS + "("
                + COLUMN_TEACHER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TEACHER_NAME + " TEXT,"
                + COLUMN_TEACHER_ADDRESS + " TEXT,"
                + COLUMN_TEACHER_PHONE + " TEXT,"
                + COLUMN_TEACHER_AGE + " INTEGER,"
                + COLUMN_TEACHER_QUALIFICATION + " TEXT,"
                + COLUMN_TEACHER_DESCRIPTION + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE_TEACHERS);

        // Confirm table creation
        System.out.println("Classes and Teachers tables created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEACHERS);
        onCreate(db);
    }

    // --------- CLASS MANAGEMENT --------- //

    // Add new class to SQLite and sync with Firebase
    public long addClass(ContentValues classDetails) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.insert(TABLE_CLASSES, null, classDetails);

        if (result != -1) {
            syncClassWithFirebase((int) result, classDetails);
        }
        db.close();
        return result;
    }

    // Delete class by ID and sync with Firebase
    public int deleteClass(int classId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CLASSES, COLUMN_ID + " = ?", new String[]{String.valueOf(classId)});

        if (rowsAffected > 0) {
            firebaseRef.child("classes").child(String.valueOf(classId)).removeValue()
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(context, "Class deleted from cloud", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to delete class from cloud", Toast.LENGTH_SHORT).show());
        }
        db.close();
        return rowsAffected;
    }

    // Update class and sync with Firebase
    public int updateClass(int classId, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.update(TABLE_CLASSES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(classId)});

        if (rowsAffected > 0) {
            syncClassWithFirebase(classId, values);
        }
        db.close();
        return rowsAffected;
    }

    // Get class by ID
    public Cursor getClassById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CLASSES, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
    }

    // Get all classes
    public Cursor getAllClasses() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CLASSES, null);
    }

    // Get classes by type
    public Cursor getClassesByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CLASSES, null, COLUMN_TYPE + "=?", new String[]{type}, null, null, null);
    }

    // Get classes by teacher name
    public Cursor getClassesByTeacher(String teacherName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CLASSES + " WHERE " + COLUMN_TEACHER + " LIKE ?", new String[]{"%" + teacherName + "%"});
    }

    // Get classes by date
    public Cursor getClassesByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CLASSES + " WHERE " + COLUMN_DAY + " = ?", new String[]{date});
    }

    // --------- SYNC WITH FIREBASE --------- //

    // Sync class with Firebase
    private void syncClassWithFirebase(int classId, ContentValues classDetails) {
        HashMap<String, Object> yogaClass = new HashMap<>();

        // Lấy tất cả giá trị từ ContentValues, bao gồm cả difficulty
        yogaClass.put("day", classDetails.getAsString(COLUMN_DAY));
        yogaClass.put("time", classDetails.getAsString(COLUMN_TIME));
        yogaClass.put("capacity", classDetails.getAsInteger(COLUMN_CAPACITY));
        yogaClass.put("duration", classDetails.getAsString(COLUMN_DURATION));
        yogaClass.put("price", classDetails.getAsDouble(COLUMN_PRICE));
        yogaClass.put("type", classDetails.getAsString(COLUMN_TYPE));
        yogaClass.put("teacher", classDetails.getAsString(COLUMN_TEACHER));
        yogaClass.put("difficulty", classDetails.getAsString(COLUMN_DIFFICULTY)); // Đảm bảo difficulty được đưa vào HashMap
        yogaClass.put("description", classDetails.getAsString(COLUMN_DESCRIPTION));


    }


    // --------- TEACHER MANAGEMENT --------- //

    // Add new teacher and sync with Firebase
    public long addTeacher(ContentValues teacherDetails) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.insert(TABLE_TEACHERS, null, teacherDetails);

        if (result != -1) {
            syncTeacherWithFirebase((int) result, teacherDetails);
        }
        db.close();
        return result;
    }

    // Delete teacher by ID and sync with Firebase, including all classes assigned to that teacher
    public int deleteTeacher(int teacherId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;

        // Start a transaction to ensure all deletions are processed together
        db.beginTransaction();
        try {
            // First, delete all classes assigned to this teacher
            Cursor classesToDelete = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_CLASSES
                    + " WHERE " + COLUMN_TEACHER + " = (SELECT " + COLUMN_TEACHER_NAME + " FROM "
                    + TABLE_TEACHERS + " WHERE " + COLUMN_TEACHER_ID + " = ?)", new String[]{String.valueOf(teacherId)});

            // Iterate over all classes to delete from Firebase as well
            int columnIndex = classesToDelete.getColumnIndex(COLUMN_ID);
            if (columnIndex != -1) {
                while (classesToDelete.moveToNext()) {
                    int classId = classesToDelete.getInt(columnIndex);
                    firebaseRef.child("classes").child(String.valueOf(classId)).removeValue();
                }
            }
            classesToDelete.close();

            // Delete classes from SQLite
            db.delete(TABLE_CLASSES, COLUMN_TEACHER + " = (SELECT " + COLUMN_TEACHER_NAME
                            + " FROM " + TABLE_TEACHERS + " WHERE " + COLUMN_TEACHER_ID + " = ?)",
                    new String[]{String.valueOf(teacherId)});

            // Then, delete the teacher
            rowsAffected = db.delete(TABLE_TEACHERS, COLUMN_TEACHER_ID + " = ?", new String[]{String.valueOf(teacherId)});

            // If teacher was successfully deleted, sync with Firebase
            if (rowsAffected > 0) {
                firebaseRef.child("teachers").child(String.valueOf(teacherId)).removeValue()
                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Teacher and assigned classes deleted from cloud", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete teacher and classes from cloud", Toast.LENGTH_SHORT).show());
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
        return rowsAffected;
    }

    // Update teacher and sync with Firebase
    public int updateTeacher(int teacherId, ContentValues teacherDetails) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.update(TABLE_TEACHERS, teacherDetails, COLUMN_TEACHER_ID + " = ?", new String[]{String.valueOf(teacherId)});

        if (rowsAffected > 0) {
            syncTeacherWithFirebase(teacherId, teacherDetails);
        }
        db.close();
        return rowsAffected;
    }

    // Get all teachers
    public Cursor getAllTeachers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TEACHERS, null);
    }

    // Get teacher by ID
    public Cursor getTeacherById(int teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_TEACHERS, null, COLUMN_TEACHER_ID + "=?", new String[]{String.valueOf(teacherId)}, null, null, null);
    }

    // Sync teacher with Firebase
    private void syncTeacherWithFirebase(int teacherId, ContentValues teacherDetails) {
        HashMap<String, Object> teacher = new HashMap<>();
        teacher.put("name", teacherDetails.getAsString(COLUMN_TEACHER_NAME));
        teacher.put("address", teacherDetails.getAsString(COLUMN_TEACHER_ADDRESS));
        teacher.put("phone", teacherDetails.getAsString(COLUMN_TEACHER_PHONE));
        teacher.put("age", teacherDetails.getAsInteger(COLUMN_TEACHER_AGE));
        teacher.put("qualification", teacherDetails.getAsString(COLUMN_TEACHER_QUALIFICATION));
        teacher.put("description", teacherDetails.getAsString(COLUMN_TEACHER_DESCRIPTION));

        firebaseRef.child("teachers").child(String.valueOf(teacherId)).setValue(teacher)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Teacher synced with cloud", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to sync teacher with cloud", Toast.LENGTH_SHORT).show());
    }

    // --------- ADDED FUNCTIONS FOR TEACHER DROPDOWN --------- //

    // Get all teacher names as a Cursor for UI components
    public Cursor getAllTeacherNamesCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COLUMN_TEACHER_NAME + " FROM " + TABLE_TEACHERS, null);

        Log.d("DB", "Number of teachers: " + cursor.getCount()); // Log the number of teachers fetched
        return cursor;
    }

    // Get all teacher names from the database for the dropdown (returns ArrayList)
    public ArrayList<String> getAllTeacherNames() {
        ArrayList<String> teacherNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COLUMN_TEACHER_NAME + " FROM " + TABLE_TEACHERS, null);

        if (cursor.moveToFirst()) {
            do {
                teacherNames.add(cursor.getString(0)); // Add teacher name to the list
            } while (cursor.moveToNext());
        }
        cursor.close();
        return teacherNames;
    }

    // Search teacher names matching input for AutoCompleteTextView
    public Cursor getTeacherNamesLike(String input) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT DISTINCT " + COLUMN_TEACHER_NAME + " FROM " + TABLE_TEACHERS + " WHERE " + COLUMN_TEACHER_NAME + " LIKE ?", new String[]{"%" + input + "%"});
    }

    // Search teacher by name (missing method added)
    public Cursor searchTeacherByName(String teacherName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_TEACHERS, null, COLUMN_TEACHER_NAME + " LIKE ?", new String[]{"%" + teacherName + "%"}, null, null, null);
    }
}
