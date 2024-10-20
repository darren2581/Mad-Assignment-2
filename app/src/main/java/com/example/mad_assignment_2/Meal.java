package com.example.mad_assignment_2;

public class Meal {

    private String name;
    private double calories;
    private double weight;
    private double carbs;
    private double protein;
    private double fat;
    private String date;

    public Meal() {
        this.name = "";
        this.calories = 0.0;
        this.weight = 0.0;
        this.carbs = 0.0;
        this.protein = 0.0;
        this.fat = 0.0;
        this.date = "";
    }

    public Meal(String name, double calories, double weight, double carbs, double protein, double fat, String date) {
        this.name = name;
        this.calories = calories;
        this.weight = weight;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.date = date;
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public double getCalories() {
        return calories;
    }

    public double getWeight() {
        return weight;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getProtein() {
        return protein;
    }

    public double getFat() {
        return fat;
    }

    public String getDate() {
        return date;
    }
}
