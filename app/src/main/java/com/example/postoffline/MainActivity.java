package com.example.postoffline;




import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postoffline.Adapter.MyAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;
    private static final String SHARED_PREFS_NAME = "MyPrefs";
    private static final String SELECTED_WEIGHT_KEY = "selectedWeight";
    private static final String SELECTED_POINT_KEY = "selectedPoint";
    private RecyclerView recyclerView;
    private GroupedAdapter adapter;
    private List<Item> itemList;
    private FloatingActionButton fabAddPost;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAddPost = findViewById(R.id.fab_add_post);
        fabAddPost.setOnClickListener(v -> {
           showPopupMenu();
        });

        loadDatadate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDatadate();
        List<String> allDates= databaseHelper.getAllDates();
        Comparator<String> dateStringComparator = new Comparator<String>() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy");

            @Override
            public int compare(String date1, String date2) {
                try {
                    Date parsedDate1 = dateFormat.parse(date1);
                    Date parsedDate2 = dateFormat.parse(date2);
                    return parsedDate1.compareTo(parsedDate2);
                } catch (ParseException e) {
                    e.printStackTrace();
                    // If parsing fails, return 0 (indicating equal)
                    return 0;
                }
            }
        };

// Sort the list of dates using the custom comparator
        Collections.sort(allDates, dateStringComparator);
        for (int i = 0; i < allDates.size(); i++) {

            System.out.println("all dates=="+allDates.get(i));
        }
    }

 /*   private void loadData() {
        itemList = new ArrayList<>();
        Cursor cursor = databaseHelper.getAllPosts();
        if (cursor.moveToFirst()) {
            do {
                String weight = cursor.getString(cursor.getColumnIndex("weight"));
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                byte[] photo = cursor.getBlob(cursor.getColumnIndex("photo"));

                itemList.add(new Item(id,photo, weight, date));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new MyAdapter(this, itemList,databaseHelper);
        recyclerView.setAdapter(adapter);
    }*/
 private void loadDatadate() {
     List<DateGroup> dateGroups = new ArrayList<>();
     Cursor cursor = databaseHelper.getPostsByDate();
     Map<String, List<Item>> tempDateGroups = new HashMap<>();

     if (cursor.moveToFirst()) {
         do {
             String date = cursor.getString(cursor.getColumnIndex("date"));
             String weight = cursor.getString(cursor.getColumnIndex("weight"));
             int id = cursor.getInt(cursor.getColumnIndex("id"));
             String photo = cursor.getString(cursor.getColumnIndex("photo"));

             Item item = new Item(id, photo, weight, date);
             if (!tempDateGroups.containsKey(date)) {
                 tempDateGroups.put(date, new ArrayList<>());
             }
             tempDateGroups.get(date).add(item);
         } while (cursor.moveToNext());

         cursor.close();

         // Convert tempDateGroups to List<DateGroup>
         for (Map.Entry<String, List<Item>> entry : tempDateGroups.entrySet()) {
             DateGroup dateGroup = new DateGroup(entry.getKey(), entry.getValue());
             dateGroups.add(dateGroup);
         }

         // Sort the dateGroups list based on date
         Collections.sort(dateGroups, new Comparator<DateGroup>() {
             DateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.ENGLISH);

             @Override
             public int compare(DateGroup group1, DateGroup group2) {
                 try {
                     Date date1 = dateFormat.parse(group1.getDate());
                     Date date2 = dateFormat.parse(group2.getDate());
                     return date2.compareTo(date1); // Descending order
                 } catch (ParseException e) {
                     e.printStackTrace();
                 }
                 return 0;
             }
         });

         adapter = new GroupedAdapter(MainActivity.this, dateGroups);
         recyclerView.setAdapter(adapter);
     }
 }


    private void showPopupMenu() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_menu);

        Button btnCamera = dialog.findViewById(R.id.btn_camera);
        Button btnWeight = dialog.findViewById(R.id.btn_weight);
        Button btnPhotoLibrary = dialog.findViewById(R.id.btn_photo_library);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnCamera.setOnClickListener(v -> {


            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                // Permission is granted, start the camera
                openCamera();
            }
            dialog.dismiss();
        });

        btnWeight.setOnClickListener(v -> {
            showNumberPickerDialog();
            dialog.dismiss();
        });

        btnPhotoLibrary.setOnClickListener(v -> {
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK);
            pickPhotoIntent.setType("image/*");  // Specify the MIME type for images
            startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
 }

    private void showNumberPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_number_picker, null);
        builder.setView(dialogView);

        NumberPicker numberPicker = dialogView.findViewById(R.id.number_picker);
        numberPicker.setMinValue(40);
        numberPicker.setMaxValue(400);
        numberPicker.setValue(100);
        numberPicker.setWrapSelectorWheel(false);

        NumberPicker numberPoint = dialogView.findViewById(R.id.number_point);
        numberPoint.setMinValue(0);
        numberPoint.setMaxValue(9);
        numberPoint.setValue(0);
        numberPoint.setWrapSelectorWheel(false);


        // Retrieve selected weight and point from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        int selectedWeight1 = sharedPreferences.getInt(SELECTED_WEIGHT_KEY, 100);
        int selectedPoint1 = sharedPreferences.getInt(SELECTED_POINT_KEY, 0);

        numberPicker.setValue(selectedWeight1);
        numberPoint.setValue(selectedPoint1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.ENGLISH);
        String timeStamp = dateFormat.format(Calendar.getInstance().getTime());
        String weight = databaseHelper.getLastNonEmptyWeight(timeStamp);

        if (weight != null && !weight.isEmpty()) {

            String[] weightParts = weight.split("\\.");

            if (weightParts.length == 2) {
                numberPicker.setValue(Integer.parseInt(weightParts[0]));
                numberPoint.setValue(Integer.parseInt(weightParts[1]));
            } else if (weightParts.length == 1) {
                numberPicker.setValue(Integer.parseInt(weightParts[0]));
            }
        }
        Button btnDone = dialogView.findViewById(R.id.btn_done);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnDone.setOnClickListener(v -> {
            int selectedValue = numberPicker.getValue();
            int selectedPoint = numberPoint.getValue();
            String selectedWeight = selectedValue + "." + selectedPoint;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(SELECTED_WEIGHT_KEY, selectedValue);
            editor.putInt(SELECTED_POINT_KEY, selectedPoint);
            editor.apply();

            addFirstRecord(timeStamp);

            boolean isInserted = databaseHelper.addPost(selectedWeight, timeStamp, "");
            if (isInserted)
            {
                loadDatadate();
                Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show();
            }
            // Do something with the selected weight
            dialog.dismiss();
        });
    }


    // Save the image to the app's internal storage directory
    private String saveImageToAppDir(Bitmap imageBitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";

        // Get the directory where you want to save the image
        File storageDir = new File(getFilesDir(), "images");
        if (!storageDir.exists()) {
            storageDir.mkdirs(); // Create the directory if it doesn't exist
        }
        File imageFile = new File(storageDir, imageFileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {


            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            String imagePath = saveImageToAppDir(imageBitmap);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.ENGLISH);  String timeStamp = dateFormat.format(Calendar.getInstance().getTime());

            addFirstRecord(timeStamp);
            boolean isInserted = databaseHelper.addPost("", timeStamp, imagePath);
            if (isInserted)
            {
                loadDatadate();
                Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show();
            }
            // Save the photo to the database or handle it as needed
        }  else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {

            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                String imagePath = saveImageToAppDir(bitmap);
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.ENGLISH);  String timeStamp = dateFormat.format(Calendar.getInstance().getTime());
                addFirstRecord(timeStamp);
                boolean isInserted = databaseHelper.addPost("", timeStamp, imagePath);
                if (isInserted)
                {
                    loadDatadate();
                    Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void addFirstRecord(String currentDate) {

        SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedDate = sharedPref.getString("savedDate", "");

// If savedDate is not empty, use it
        if (!savedDate.isEmpty()) {
            // Use the saved date as needed
            Log.d("SavedDate", "Saved Date: " + savedDate);
        } else {
            // Handle the case when no date is saved
            Log.d("SavedDate", "No date saved in SharedPreferences");

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("savedDate", currentDate);
            editor.apply();
        }

    }
}
