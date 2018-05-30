package com.example.jonth.simplefoodlogging;
import org.joda.time.DateTime;

import java.util.Date;

public class FoodEntry
{
    //    logType: foodIntake
//    Time:
//    contents:
//    {
//        time: start
//        name:
//        duration:
//        nutrition:
//        quantity:
//        mealType:
//        emotion:
//        metadata: {
//            img:
//            audio:
//            recipe:
//            restaurant:
//        }
//    }

    private String name;
    private DateTime time;
    private float duration;
    private int quantity;
    private String nutrition;
    private MainActivity.MealType mealType;
    private String emotion;

    public FoodEntry(String name, DateTime time, int quantity, MainActivity.MealType mealType, String emotion) {
        this.name = name;
        this.time = time;
        this.quantity = quantity;
        this.mealType = mealType;
        this.emotion = emotion;
    }

    public FoodEntry() {
        this.name = "";
        this.time = new DateTime();
        this.quantity = 1;
        this.mealType = MainActivity.MealType.NONE;
        this.emotion = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public MainActivity.MealType getMealType() {
        return mealType;
    }

    public void setMealType(MainActivity.MealType mealType) {
        this.mealType = mealType;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
}
