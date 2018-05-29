package com.example.jonth.simplefoodlogging;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

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

        Response response = analyzeQuery("I ate 2 bowls of chicken tikka masala.");
        List<Entity> foods = findFoods(response);
        int count = findCount(response);
        MealType mealType = findMealType(response);
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

    protected int findCount(Response response) {
        List<Word> words = response.getWords();

        for(Word word: words){
            if(word.getPartOfSpeech().contains("CD")){
                return Integer.parseInt(word.getToken());
            }
        }

        return 1;
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
