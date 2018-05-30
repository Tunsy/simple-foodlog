package com.example.jonth.simplefoodlogging;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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

        mRecyclerView = (RecyclerView) findViewById(R.id.breakfast_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        // mAdapter = new MyAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

        // Initialize TextRazor
        client = new TextRazor(BuildConfig.ApiKey);
        client.addExtractor("words");
        client.addExtractor("entities");

        Response response = analyzeQuery("I ate 2 bowls of chicken tikka masala and 3 coca colas for dinner.");
        List<Entity> foods = findFoods(response);
        List<Word> quantities = findQuantities(response);
        MealType mealType = findMealType(response);
        List<FoodEntry> foodEntries = createFoodEntries(foods, quantities, mealType);
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
                    return MealType.valueOf(keyword.getEntityId());
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
