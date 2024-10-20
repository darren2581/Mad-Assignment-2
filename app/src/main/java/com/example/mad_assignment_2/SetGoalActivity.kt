package com.example.mad_assignment_2

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class SetGoalActivity : AppCompatActivity() {

    private lateinit var goalInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var saveGoalButton: Button
    private lateinit var currentCalorie: TextView
    private lateinit var currentDate: TextView
    private val db = FirebaseFirestore.getInstance()

    private var goal: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_goal)

        goalInput = findViewById(R.id.goalInput)
        dateInput = findViewById(R.id.date)
        saveGoalButton = findViewById(R.id.saveGoalButton)
        currentCalorie = findViewById(R.id.currentCalorie)
        currentDate = findViewById(R.id.currentDate)

        loadPreferences()

        val mealCalories = intent.getDoubleExtra("mealCalories", 0.0)
        currentCalorie.text = "${mealCalories} kCal" // Initial calorie display

        savedInstanceState?.let {
            val savedCalories = it.getString("currentCalories") ?: "0.0 kCal"
            currentCalorie.text = savedCalories // Restore the currentCalories value
        }

        saveGoalButton.setOnClickListener {
            val dateValue = dateInput.text.toString()
            val goalValue = goalInput.text.toString().toIntOrNull()

            if (goalValue != null && dateValue.isNotBlank()) {
                goal = goalValue

                currentDate.text = dateValue

                fetchCaloriesForDate(dateValue)

                saveDatePreference(dateValue)
            } else {
                Toast.makeText(this, "Please enter a valid number and date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCaloriesForDate(date: String) {
        db.collection("Meals")
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { result ->
                var totalCalories = 0.0
                for (document in result) {
                    val meal = document.toObject<Meal>()
                    totalCalories += meal.calories
                }
                updateCalorieDisplay(totalCalories)

                // Save the goal and calories to SharedPreferences
                saveCalorieGoal(goal, date, totalCalories)
            }
            .addOnFailureListener { exception ->
                // Handle the error
                Toast.makeText(this, "Error fetching data: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    // Save daily goal and date to SharedPreferences
    private fun saveCalorieGoal(goal: Int, date: String, currentCalories: Double) {
        val sharedPref = getSharedPreferences("CaloriePrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("dailyCalorieGoal", goal)
        editor.putString("currentCalories", "${currentCalories} kCal")
        editor.apply()
    }

    // Save date to SharedPreferences
    private fun saveDatePreference(date: String) {
        val sharedPref = getSharedPreferences("CaloriePrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("dailyCalorieDate", date)
        editor.apply()
    }

    // Load saved preferences
    private fun loadPreferences() {
        val sharedPref = getSharedPreferences("CaloriePrefs", Context.MODE_PRIVATE)
        goal = sharedPref.getInt("dailyCalorieGoal", 0)
        val savedDate = sharedPref.getString("dailyCalorieDate", "Not Set") ?: "Not Set"
        val savedCalories = sharedPref.getString("currentCalories", "0.0 kCal") ?: "0.0 kCal"

        goalInput.setText(goal.toString())
        currentCalorie.text = savedCalories
        dateInput.setText(savedDate)
        updateDateDisplay(savedDate)
    }

    // Update currentCalorie TextView
    private fun updateCalorieDisplay(totalCalories: Double) {
        currentCalorie.text = "${totalCalories} kCal / ${goal} kCal"
    }

    // Update currentDate TextView
    private fun updateDateDisplay(date: String) {
        currentDate.text = date
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("goalInput", goalInput.text.toString())
        outState.putString("dateInput", dateInput.text.toString())
        outState.putString("currentCalories", currentCalorie.text.toString())
    }
}

