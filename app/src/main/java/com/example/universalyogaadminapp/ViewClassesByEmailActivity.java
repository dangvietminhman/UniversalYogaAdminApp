package com.example.universalyogaadminapp;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.graphics.Canvas;
import android.graphics.Color;

public class ViewClassesByEmailActivity extends AppCompatActivity {

    private ListView listViewClasses;
    private ArrayAdapter<String> classesAdapter;
    private ArrayList<String> classesList;
    private DatabaseReference ordersRef;
    private String selectedEmail;
    private Button buttonExportInvoice;
    private double totalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_classes_by_email);

        // Get the selected email from the intent
        selectedEmail = getIntent().getStringExtra("email");

        // Initialize views
        listViewClasses = findViewById(R.id.listViewClasses);
        buttonExportInvoice = findViewById(R.id.buttonExportInvoice); // Button to export invoice
        classesList = new ArrayList<>();

        // Back button functionality
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish()); // Finishes the current activity to go back

        // Set up ArrayAdapter for the ListView
        classesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classesList);
        listViewClasses.setAdapter(classesAdapter);

        // Firebase reference to "orders" node
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Load classes booked by the selected email
        loadClassesByEmail();

        // Set up the export button to create PDF
        buttonExportInvoice.setOnClickListener(v -> createInvoicePDF());
    }

    private void loadClassesByEmail() {
        if (selectedEmail == null) {
            Toast.makeText(this, "No email found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classesList.clear();
                totalAmount = 0; // Reset total amount

                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String email = orderSnapshot.child("Email").getValue(String.class);
                    if (selectedEmail.equals(email)) {
                        // Loop through each class under "Classes" for the matching email
                        for (DataSnapshot classSnapshot : orderSnapshot.child("Classes").getChildren()) {
                            String classType = classSnapshot.child("Type").getValue(String.class);
                            String day = classSnapshot.child("Day").getValue(String.class);
                            String time = classSnapshot.child("Time").getValue(String.class);
                            String teacher = classSnapshot.child("Teacher").getValue(String.class);

                            // Retrieve Price and handle null values
                            double price = classSnapshot.child("Price").exists() ?
                                    classSnapshot.child("Price").getValue(Double.class) : 0;
                            totalAmount += price;

                            // Format class info with type, day, time, teacher, and price
                            String classInfo = "Type: " + classType +
                                    ", Day: " + day +
                                    ", Time: " + time +
                                    ", Teacher: " + teacher +
                                    ", Price: $" + price;

                            classesList.add(classInfo);
                        }
                    }
                }

                classesAdapter.notifyDataSetChanged();

                if (classesList.isEmpty()) {
                    Toast.makeText(ViewClassesByEmailActivity.this, "No classes found for this email.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewClassesByEmailActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createInvoicePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        int yPosition = 50;

        // Title and Date/Time
        paint.setTextSize(20);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("INVOICE", 240, yPosition, paint);

        // Date and Time
        String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        yPosition += 30;
        canvas.drawText("Date: " + dateTime, 20, yPosition, paint);
        canvas.drawText("Email: " + selectedEmail, 400, yPosition, paint);

        // Divider Line
        yPosition += 20;
        paint.setStrokeWidth(1);
        canvas.drawLine(20, yPosition, 575, yPosition, paint);

        // Column Headers
        yPosition += 30;
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Class Type", 20, yPosition, paint);
        canvas.drawText("Day", 150, yPosition, paint);
        canvas.drawText("Time", 250, yPosition, paint);
        canvas.drawText("Teacher", 350, yPosition, paint);
        canvas.drawText("Price", 500, yPosition, paint);

        // Divider Line under headers
        yPosition += 10;
        paint.setStrokeWidth(1);
        canvas.drawLine(20, yPosition, 575, yPosition, paint);

        // Reset text style for content
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        yPosition += 20;

        // Itemized list of booked classes
        for (String classInfo : classesList) {
            String[] details = classInfo.split(", ");
            canvas.drawText(details[0].replace("Type: ", ""), 20, yPosition, paint);
            canvas.drawText(details[1].replace("Day: ", ""), 150, yPosition, paint);
            canvas.drawText(details[2].replace("Time: ", ""), 250, yPosition, paint);
            canvas.drawText(details[3].replace("Teacher: ", ""), 350, yPosition, paint);
            canvas.drawText(details[4].replace("Price: ", ""), 500, yPosition, paint);
            yPosition += 20;
        }

        // Divider Line above total
        yPosition += 10;
        paint.setStrokeWidth(1);
        canvas.drawLine(20, yPosition, 575, yPosition, paint);

        // Total Price
        yPosition += 30;
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Total Amount:", 400, yPosition, paint);
        canvas.drawText("$" + totalAmount, 500, yPosition, paint);

        pdfDocument.finishPage(page);

        // Save the PDF to internal storage
        File file = new File(getFilesDir(), "Invoice_" + selectedEmail + ".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Invoice saved as PDF in internal storage", Toast.LENGTH_SHORT).show();
            openPDF(file);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }

    private void openPDF(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found on this device", Toast.LENGTH_SHORT).show();
        }
    }
}
