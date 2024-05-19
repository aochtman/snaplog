package com.example.postoffline;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditItemActivity extends AppCompatActivity {

    private NumberPicker numberPickerWhole, numberPickerFraction;
    private ImageView imageView;
    private Button btnSave, btnDelete,btnUploadImage;
    private DatabaseHelper databaseHelper;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private int itemId;
    private String isImage_exist="";
    private LinearLayout lv_tv_number,lv_image;
    String image="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);
        btnUploadImage = findViewById(R.id.button_upload_image);
        lv_tv_number = findViewById(R.id.lv_tv_number);
        lv_image=findViewById(R.id.lv_image);
        numberPickerWhole = findViewById(R.id.number_picker);
        numberPickerFraction = findViewById(R.id.number_point);
        imageView = findViewById(R.id.image_view);
        btnSave = findViewById(R.id.button_save);
        btnDelete = findViewById(R.id.button_delete);
        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        itemId = intent.getIntExtra("itemId", -1);
        String weight = intent.getStringExtra("weight");
        String date = intent.getStringExtra("date");
        isImage_exist = intent.getStringExtra("image");

        System.out.println("dkjkajdkf=="+weight+"     image"+isImage_exist);
        // Initialize NumberPickers
        numberPickerFraction.setMinValue(0);
        numberPickerFraction.setMaxValue(9);
        numberPickerFraction.setValue(0);
        numberPickerWhole.setMinValue(40);
        numberPickerWhole.setMaxValue(400);
        numberPickerWhole.setValue(100);

        if (weight != null && !weight.isEmpty()) {
            lv_tv_number.setVisibility(View.VISIBLE);
            lv_image.setVisibility(View.GONE);
            String[] weightParts = weight.split("\\.");

            if (weightParts.length == 2) {
                numberPickerWhole.setValue(Integer.parseInt(weightParts[0]));
                numberPickerFraction.setValue(Integer.parseInt(weightParts[1]));
            } else if (weightParts.length == 1) {
                numberPickerWhole.setValue(Integer.parseInt(weightParts[0]));
            }
        }
        btnUploadImage.setOnClickListener(v -> {
            showPopupMenu();
        });

        if (isImage_exist != null) {
            if (isImage_exist.equals("contain"))
            {
                image = databaseHelper.getPhotoById(itemId);
                if (!image.isEmpty()) {

                    Uri imageUri = Uri.parse(image);
                    imageView.setImageURI(imageUri);
                    lv_image.setVisibility(View.VISIBLE);
                    lv_tv_number.setVisibility(View.GONE);
                }


            }

        } else {
            lv_image.setVisibility(View.GONE);
            lv_tv_number.setVisibility(View.VISIBLE);
        }

        btnSave.setOnClickListener(v -> {
            int whole = numberPickerWhole.getValue();
            int fraction = numberPickerFraction.getValue();
            String newWeight = whole + "." + fraction;
            databaseHelper.updateItem(itemId, newWeight, date, image);
            setResult(RESULT_OK);
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            databaseHelper.deleteItem(itemId);
            setResult(RESULT_OK);
            finish();
        });
    }

    private void showPopupMenu() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_menu_edit);

        Button btnCamera = dialog.findViewById(R.id.btn_camera);

        Button btnPhotoLibrary = dialog.findViewById(R.id.btn_photo_library);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnCamera.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
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

            image = saveImageToAppDir(imageBitmap);
            imageView.setImageBitmap(imageBitmap);
            // Save the photo to the database or handle it as needed
        }  else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {

            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);

                image = saveImageToAppDir(bitmap);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
