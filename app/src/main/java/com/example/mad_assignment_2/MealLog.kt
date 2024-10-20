package com.example.mad_assignment_2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class MealLog : AppCompatActivity() {

    private lateinit var mealAdapter: MealAdapter
    private val mealsList = mutableListOf<Meal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meal_log)
        // Set up RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        mealAdapter = MealAdapter(mealsList)
        recyclerView.adapter = mealAdapter

        // Initialize Firestore
        val db = FirebaseFirestore.getInstance()

        // Fetch meals from Firestore
        db.collection("Meals")
            .get()
            .addOnSuccessListener { result ->
                // Iterate through the result and convert to Meal objects
                for (document in result) {
                    val meal = document.toObject<Meal>()
                    mealsList.add(meal) // Add the Meal object to the list
                }
                // Notify the adapter that the data has changed
                mealAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle the error
                println("Error getting documents: $exception")
            }
    }
}