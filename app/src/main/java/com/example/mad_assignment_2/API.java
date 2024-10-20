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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class API extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();
    private FirebaseFirestore db;
    private Meal currentMeal;
    private final int IMAGE_PICK_CODE = 1000;
    private final int CAMERA_REQUEST_CODE = 1001;
    private ImageView mealImageView;
    private Uri selectedImageUri;

    private static final String API_KEY = "dIpWa0zVG+i/cw+G1K4JmQ==BjN6VAKFykMMgPgv";
    private static final String API_URL = "https://api.calorieninjas.com/v1/nutrition?query=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        EditText foodInput = findViewById(R.id.foodInput);
        EditText foodGram = findViewById(R.id.foodGram);
        EditText date = findViewById(R.id.date);
        Button searchButton = findViewById(R.id.searchButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button manualButton = findViewById(R.id.manualButton);
        Button photoButton = findViewById(R.id.photoBtn);
        TextView resultTextView = findViewById(R.id.resultTextView);
        mealImageView = findViewById(R.id.mealImageView);

        searchButton.setOnClickListener(v -> {
            String foodName = foodInput.getText().toString();
            String foodGramString = foodGram.getText().toString();
            String foodDate = date.getText().toString();

            if (!foodName.isEmpty() && !foodGramString.isEmpty()) {
                double grams = Double.parseDouble(foodGramString);
                searchFood(foodName, grams, resultTextView, foodDate);
            } else {
                resultTextView.setText("Please enter both food name and grams.");
            }
        });

        saveButton.setOnClickListener(v -> {
            if (currentMeal != null) {
                saveMealToFirestore(currentMeal);
                startActivity(new Intent(API.this, Menu.class));
            } else {
                resultTextView.setText("No meal to save. Please search for a meal first.");
            }
        });

        manualButton.setOnClickListener(v -> startActivity(new Intent(API.this, Manual.class)));

        photoButton.setOnClickListener(v -> {
            String[] options = {"Gallery", "Camera"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Image Source")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openGallery();
                        } else if (which == 1) {
                            openCamera();
                        }
                    })
                    .show();
        });

        // Restore saved instance state if available
        if (savedInstanceState != null) {
            // Restore text fields
            foodInput.setText(savedInstanceState.getString("foodName"));
            foodGram.setText(savedInstanceState.getString("foodGram"));
            date.setText(savedInstanceState.getString("date"));

            // Restore the result text
            resultTextView.setText(savedInstanceState.getString("resultText"));

            // Restore the meal object
            if (savedInstanceState.getString("mealName") != null) {
                currentMeal = new Meal(
                        savedInstanceState.getString("mealName"),
                        savedInstanceState.getDouble("mealCalories"),
                        savedInstanceState.getDouble("mealGrams"),
                        savedInstanceState.getDouble("mealCarbs"),
                        savedInstanceState.getDouble("mealProtein"),
                        savedInstanceState.getDouble("mealFat"),
                        savedInstanceState.getString("mealDate")
                );
            }

            // Restore image URI if exists
            String imageUriString = savedInstanceState.getString("imageUri");
            if (imageUriString != null) {
                selectedImageUri = Uri.parse(imageUriString);
                mealImageView.setImageURI(selectedImageUri);
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
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                mealImageView.setImageBitmap(bitmap);
            }
        }
    }

    private void searchFood(String foodName, double grams, TextView resultTextView, String foodDate) {
        String url = API_URL + foodName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Api-Key", API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> resultTextView.setText("Failed to fetch data."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    parseJsonData(jsonData, resultTextView, grams, foodDate);
                } else {
                    runOnUiThread(() -> resultTextView.setText("Failed to fetch data: " + response.code()));
                }
            }
        });
    }

    private void parseJsonData(String responseData, TextView resultTextView, double grams, String date) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray items = jsonObject.getJSONArray("items");
            if (items.length() > 0) {
                JSONObject item = items.getJSONObject(0);
                String name = item.getString("name");
                double calories = item.getDouble("calories");
                double carbs = item.getDouble("carbohydrates_total_g");
                double protein = item.getDouble("protein_g");
                double fat = item.getDouble("fat_total_g");

                double totalCalories = (calories / 100) * grams;
                double totalCarbs = (carbs / 100) * grams;
                double totalProtein = (protein / 100) * grams;
                double totalFat = (fat / 100) * grams;

                String result = "Date: " + date + "\n" +
                        "Food: " + name + "\n" +
                        "Calories (for " + grams + "g): " + totalCalories + " kcal\n" +
                        "Carbs: " + totalCarbs + " g\n" +
                        "Protein: " + totalProtein + " g\n" +
                        "Fat: " + totalFat + " g";

                runOnUiThread(() -> {
                    resultTextView.setText(result);
                    currentMeal = new Meal(name, totalCalories, grams, totalCarbs, totalProtein, totalFat, date);
                });
            } else {
                runOnUiThread(() -> resultTextView.setText("No results found."));
            }
        } catch (Exception e) {
            runOnUiThread(() -> resultTextView.setText("Error parsing data: " + e.getMessage()));
        }
    }

    private void saveMealToFirestore(Meal meal) {
        // Prepare the meal data
        Map<String, Object> mealData = new HashMap<>();
        mealData.put("name", meal.getName());
        mealData.put("calories", meal.getCalories());
        mealData.put("weight", meal.getWeight());
        mealData.put("carbs", meal.getCarbs());
        mealData.put("protein", meal.getProtein());
        mealData.put("fat", meal.getFat());
        mealData.put("date", meal.getDate());
        mealData.put("imageUrl", ""); // Set imageUrl to an empty string

        // Save to Firestore
        db.collection("Meals")
                .add(mealData)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("DocumentSnapshot added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error adding document: " + e);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save text fields
        outState.putString("foodName", ((EditText) findViewById(R.id.foodInput)).getText().toString());
        outState.putString("foodGram", ((EditText) findViewById(R.id.foodGram)).getText().toString());
        outState.putString("date", ((EditText) findViewById(R.id.date)).getText().toString());

        // Save the result text
        outState.putString("resultText", ((TextView) findViewById(R.id.resultTextView)).getText().toString());

        // Save meal object if it exists
        if (currentMeal != null) {
            outState.putString("mealName", currentMeal.getName());
            outState.putDouble("mealCalories", currentMeal.getCalories());
            outState.putDouble("mealGrams", currentMeal.getWeight());
            outState.putDouble("mealCarbs", currentMeal.getCarbs());
            outState.putDouble("mealProtein", currentMeal.getProtein());
            outState.putDouble("mealFat", currentMeal.getFat());
            outState.putString("mealDate", currentMeal.getDate());
        }

        // Save the image URI (if the image is from gallery)
        if (selectedImageUri != null) {
            outState.putString("imageUri", selectedImageUri.toString());
        }
    }
}
