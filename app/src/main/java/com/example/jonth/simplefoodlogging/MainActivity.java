package com.example.jonth.simplefoodlogging;

import android.app.Dialog;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.textrazor.AnalysisException;
import com.textrazor.NetworkException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.AnalyzedText;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.Response;
import com.textrazor.annotations.Word;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private TextRazor client;
    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK, NONE
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize TextRazor
        client = new TextRazor(BuildConfig.ApiKey);
        client.addExtractor("words");
        client.addExtractor("entities");
        handleQuery("I ate 2 bowls of chicken tikka masala and 3 coca colas for dinner.");
        handleQuery("I ate 2 pizzas and an egg for breakfast.");

//
//        Dialog dialog=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
//        dialog.setContentView(R.layout.frame_help);
//        dialog.show();
    }

    protected void handleQuery(String query) {
        Response response = analyzeQuery(query);
        List<Entity> foods = findFoods(response);
        List<Word> quantities = findQuantities(response);
        MealType mealType = findMealType(response);
        List<FoodEntry> foodEntries = createFoodEntries(foods, quantities, mealType);
        displayMeals(foodEntries);
    }

    protected void displayMeals(List<FoodEntry> foodEntries) {
        LinearLayout breakfastLayout = (LinearLayout) findViewById(R.id.breakfast_linear_layout_view);
        LinearLayout lunchLayout = (LinearLayout) findViewById(R.id.lunch_linear_layout_view);
        LinearLayout dinnerLayout = (LinearLayout) findViewById(R.id.dinner_linear_layout_view);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FoodEntry item : foodEntries) {
            View view;
            LinearLayout linearLayout;
            if (item.getMealType() == MealType.BREAKFAST) {
                view  = inflater.inflate(R.layout.food_entry_view, breakfastLayout, false);
                linearLayout = breakfastLayout;
            } else if (item.getMealType() == MealType.LUNCH) {
                view  = inflater.inflate(R.layout.food_entry_view, lunchLayout, false);
                linearLayout = lunchLayout;
            } else {
                view  = inflater.inflate(R.layout.food_entry_view, dinnerLayout, false);
                linearLayout = dinnerLayout;
            }

            TextView foodname = (TextView) view.findViewById(R.id.food_name_entry);
            TextView foodQuantity = (TextView) view.findViewById(R.id.food_quantity);
            foodname.setText(item.getName());
            foodQuantity.setText(Integer.toString(item.getQuantity()) + " servings");
            // set item content in view
            linearLayout.addView(view);
        }
    }

    protected Response analyzeQuery(String query) {
        try {
            AnalyzedText response = client.analyze(query);
            return response.getResponse();
        } catch (NetworkException e) {
            e.printStackTrace();
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected List<FoodEntry> createFoodEntries(List<Entity> foods, List<Word> quantities, MealType mealType)
    {
        List<FoodEntry> foodEntries = new ArrayList<FoodEntry>();
        for(Entity foodEntity: foods) {
            FoodEntry food = new FoodEntry();
            food.setMealType(mealType);
            food.setTime(new DateTime());
            food.setQuantity(1);
            food.setName(foodEntity.getEntityId().toString());
            foodEntries.add(food);
        }


        if(quantities != null) {
            int currentSmallestQuantityDistance = Integer.MAX_VALUE;
            Entity closestFood = null;
            for(Word quantity: quantities) {
                for(Entity foodEntity: foods) {
                    if (quantity.getStartingPos() < foodEntity.getStartingPos() && foodEntity.getStartingPos() - quantity.getStartingPos() < currentSmallestQuantityDistance) {
                        currentSmallestQuantityDistance = foodEntity.getStartingPos() - quantity.getPosition();
                        closestFood = foodEntity;
                    }
                }

                for(FoodEntry food: foodEntries) {
                    if(closestFood.getEntityId().equals(food.getName())) {
                        food.setQuantity(Integer.parseInt(quantity.getToken()));
                    }
                }
            }
        }

        return foodEntries;
    }

    protected ArrayList<Entity> findFoods(Response response) {
        List<Entity> keywords = response.getEntities();
        ArrayList<Entity> foods = new ArrayList<Entity>();

        for(Entity keyword: keywords){
            List<String> itemTypes = keyword.getFreebaseTypes();

            if(stringContainsItemFromList("/food/food", itemTypes) && keyword.getConfidenceScore() > 1){
                foods.add(keyword);
            }
        }

        removeDuplicates(foods);
        return foods;
    }

    protected static List<Word> findQuantities(Response response) {
        List<Word> words = response.getWords();
        List<Word> quantities = new ArrayList<Word>();

        for(Word word: words){
            if(word.getPartOfSpeech().contains("CD")){
                quantities.add(word);
            }
        }

        return quantities;
    }

    protected MealType findMealType(Response response) {
        List<Entity> keywords = response.getEntities();

        // Find meal keyword
        for (Entity keyword : keywords) {
            List<String> itemTypes = keyword.getFreebaseTypes();

            if (stringContainsItemFromList("/dining/cuisine", itemTypes) || stringContainsItemFromList("/travel/accommodation_feature", itemTypes)) {
                try{
                    return MealType.valueOf(keyword.getEntityId().toUpperCase());
                }catch(IllegalArgumentException ex){
                    continue;
                }
            }
        }

        // Find the closest time if there was no keyword
        return findClosestMealTime(response);
    }

    protected MealType findClosestMealTime(Response response) {
        DateTime dt = new DateTime();
        int hour = dt.getHourOfDay();

        if(hour >= 6 && hour < 11){
            return MealType.BREAKFAST;
        }else if(hour >= 11 && hour < 14){
            return MealType.LUNCH;
        }else if(hour >= 17 && hour <= 21) {
            return MealType.DINNER;
        }else{
            return MealType.SNACK;
        }
    }

    protected void removeDuplicates(List<Entity> foods) {
        List<Entity> duplicates = new ArrayList<Entity>();
        Set<Integer> uniqueWordPositions = new HashSet<Integer>();
        sortByPhraseLength(foods);

        for (Entity food: foods) {
            for (Word word : food.getMatchingWords()) {
                if (uniqueWordPositions.contains(word.getPosition())) {
                    duplicates.add(food);
                    break;
                } else {
                    uniqueWordPositions.add(word.getPosition());
                }
            }
        }
        foods.removeAll(duplicates);
    }

    public void sortByPhraseLength(List<Entity> foods) {
        Collections.sort(foods, new Comparator<Entity>(){
            public int compare(Entity o1, Entity o2){
                if(o1.getMatchingWords().size() == o2.getMatchingWords().size())
                    return 0;

                return o1.getMatchingWords().size() > o2.getMatchingWords().size() ? -1 : 1;
            }
        });
    }

    public static boolean stringContainsItemFromList(String inputStr, List<String> items) {
        if(items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (inputStr.contains(items.get(i))) {
                    return true;
                }
            }
        }

        return false;
    }
}
