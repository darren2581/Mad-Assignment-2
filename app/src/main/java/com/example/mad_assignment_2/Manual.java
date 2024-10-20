package com.example.mad_assignment_2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class Manual extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView mealImageView;
    private Uri selectedImageUri;
    private Bitmap capturedImageBitmap;
    private final int IMAGE_PICK_CODE = 1000;
    private final int CAMERA_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        EditText foodInput = findViewById(R.id.foodInput);
        EditText foodGram = findViewById(R.id.foodGram);
        EditText foodCarbs = findViewById(R.id.foodCarbs);
        EditText foodProtein = findViewById(R.id.foodProtein);
        EditText foodFat = findViewById(R.id.foodFat);
        EditText foodCalories = findViewById(R.id.foodCalories);
        EditText dateInput = findViewById(R.id.date);
        mealImageView = findViewById(R.id.photoImageView);

        Button saveButton = findViewById(R.id.saveButton);
        Button photoButton = findViewById(R.id.photoBtn);

        photoButton.setOnClickListener(view -> {
            // Open a dialog to choose between Gallery and Camera
            String[] options = {"Gallery", "Camera"};
            new android.app.AlertDialog.Builder(Manual.this)
                    .setTitle("Select Image Source")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openGallery(); // Open Gallery
                        } else if (which == 1) {
                            openCamera();  // Open Camera
                        }
                    }).show();
        });

        saveButton.setOnClickListener(view -> {
            // Get values from EditText fields
            String foodName = foodInput.getText().toString();
            Double weight = tryParseDouble(foodGram.getText().toString());
            Double carbs = tryParseDouble(foodCarbs.getText().toString());
            Double protein = tryParseDouble(foodProtein.getText().toString());
            Double fat = tryParseDouble(foodFat.getText().toString());
            Double calories = tryParseDouble(foodCalories.getText().toString());
            String date = dateInput.getText().toString();

            if (foodName.isEmpty() || weight == null || carbs == null || protein == null || fat == null || calories == null || date.isEmpty()) {
                Toast.makeText(Manual.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Create a Meal object with the data
                Meal meal = new Meal(foodName, calories, weight, carbs, protein, fat, date);

                // Save the meal to Firestore
                saveMealToFirestore(meal);

                // Start the Menu activity after saving the meal
                Intent intent = new Intent(Manual.this, Menu.class);
                startActivity(intent);
            }
        });

        // Restore saved instance state if available
        if (savedInstanceState != null) {
            // Restore text fields
            foodInput.setText(savedInstanceState.getString("foodName"));
            foodGram.setText(savedInstanceState.getString("foodGram"));
            foodCarbs.setText(savedInstanceState.getString("foodCarbs"));
            foodProtein.setText(savedInstanceState.getString("foodProtein"));
            foodFat.setText(savedInstanceState.getString("foodFat"));
            foodCalories.setText(savedInstanceState.getString("foodCalories"));
            dateInput.setText(savedInstanceState.getString("date"));

            // Restore image URI if exists
            String imageUriString = savedInstanceState.getString("imageUri");
            if (imageUriString != null) {
                selectedImageUri = Uri.parse(imageUriString);
                mealImageView.setImageURI(selectedImageUri);
            }

            // Restore captured image bitmap
            if (savedInstanceState.getParcelable("capturedImageBitmap") != null) {
                capturedImageBitmap = savedInstanceState.getParcelable("capturedImageBitmap");
                mealImageView.setImageBitmap(capturedImageBitmap);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE && data != null) {
                selectedImageUri = data.getData();
                mealImageView.setImageURI(selectedImageUri);
            } else if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                capturedImageBitmap = (Bitmap) data.getExtras().get("data");
                mealImageView.setImageBitmap(capturedImageBitmap);
            }
        }
    }

    private void saveMealToFirestore(Meal meal) {
        // Get a reference to the Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        String imageFileName = System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("meals/" + imageFileName);

        if (selectedImageUri != null) {
            if (isImageFromCamera(selectedImageUri)) {
                imageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                saveMealDataToFirestore(meal, uri.toString());
                            }).addOnFailureListener(e -> {
                                System.out.println("Error getting download URL: " + e);
                            });
                        }).addOnFailureListener(e -> {
                            System.out.println("Error uploading image: " + e);
                        });
            }
            else {
                saveMealDataToFirestore(meal, "");
            }
        }
        else {
            saveMealDataToFirestore(meal, "");
        }
    }

    // Helper method to detect if an image was taken from the camera
    private boolean isImageFromCamera(Uri uri) {
        return uri.toString().startsWith("content://media/external/images/media");
    }

    // Save meal data with the provided image URL (can be empty)
    private void saveMealDataToFirestore(Meal meal, String imageUrl) {
        Map<String, Object> mealData = new HashMap<>();
        mealData.put("name", meal.getName());
        mealData.put("calories", meal.getCalories());
        mealData.put("weight", meal.getWeight());
        mealData.put("carbs", meal.getCarbs());
        mealData.put("protein", meal.getProtein());
        mealData.put("fat", meal.getFat());
        mealData.put("date", meal.getDate());
        mealData.put("imageUrl", imageUrl);

        // Save to Firestore
        db.collection("Meals")
                .add(mealData)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("DocumentSnapshot added with ID: " + documentReference.getId());
                    finish(); // Finish the activity after saving
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error adding document: " + e);
                });
    }



    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save text fields
        outState.putString("foodName", ((EditText) findViewById(R.id.foodInput)).getText().toString());
        outState.putString("foodGram", ((EditText) findViewById(R.id.foodGram)).getText().toString());
        outState.putString("foodCarbs", ((EditText) findViewById(R.id.foodCarbs)).getText().toString());
        outState.putString("foodProtein", ((EditText) findViewById(R.id.foodProtein)).getText().toString());
        outState.putString("foodFat", ((EditText) findViewById(R.id.foodFat)).getText().toString());
        outState.putString("foodCalories", ((EditText) findViewById(R.id.foodCalories)).getText().toString());
        outState.putString("date", ((EditText) findViewById(R.id.date)).getText().toString());

        // Save the image URI (if the image is from gallery)
        if (selectedImageUri != null) {
            outState.putString("imageUri", selectedImageUri.toString());
        }

        // Save the captured image bitmap (if the image is from camera)
        if (capturedImageBitmap != null) {
            outState.putParcelable("capturedImageBitmap", capturedImageBitmap);
        }
    }
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }
}
