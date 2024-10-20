package com.example.mad_assignment_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MealAdapter(private val mealList: List<Meal>) :
    RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealName: TextView = itemView.findViewById(R.id.mealName)
        val mealCalories: TextView = itemView.findViewById(R.id.mealCalories)
        val mealDate: TextView = itemView.findViewById(R.id.mealDate)
        val mealCarbs: TextView = itemView.findViewById(R.id.mealCarbs)
        val mealProtein: TextView = itemView.findViewById(R.id.mealProtein)
        val mealFat: TextView = itemView.findViewById(R.id.mealFat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = mealList[position]
        holder.mealName.text = meal.name
        holder.mealCalories.text = "Calories: ${meal.calories}"
        holder.mealDate.text = "${meal.date}"
        holder.mealCarbs.text = "Carbs: ${meal.carbs} g"
        holder.mealProtein.text = "Protein: ${meal.protein} g"
        holder.mealFat.text = "Fat: ${meal.fat} g"

        if (position == 0) {
            val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = 70
            holder.itemView.layoutParams = params
        }
    }

    override fun getItemCount() = mealList.size
}