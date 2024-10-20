package com.example.mad_assignment_2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        val addMeal = findViewById<Button>(R.id.addMeal)
        val showMeal = findViewById<Button>(R.id.showMeal)
        val dailyGoal = findViewById<Button>(R.id.dailyGoal)

        // Add Meal Button
        addMeal.setOnClickListener {
            val intent = Intent(this, API::class.java)
            startActivity(intent)
        }

        // Show Meal Button
        showMeal.setOnClickListener {
            val intent = Intent(this, MealLog::class.java)
            startActivity(intent)
        }

        dailyGoal.setOnClickListener {
            val intent = Intent(this, SetGoalActivity::class.java)
            startActivity(intent)
        }

    }
}