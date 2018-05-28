package com.example.jonth.simplefoodlogging;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.textrazor.AnalysisException;
import com.textrazor.NetworkException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.AnalyzedText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        TextRazor client = new TextRazor(BuildConfig.ApiKey);

        client.addExtractor("words");
        client.addExtractor("entities");

        AnalyzedText response = null;
        try {
            response = client.analyze("For dessert, I had two ice creams.");
        } catch (NetworkException e) {
            e.printStackTrace();
        } catch (AnalysisException e) {
            e.printStackTrace();
        }

        for (Entity entity : response.getResponse().getEntities()) {
            System.out.println("Matched Entity: " + entity.getEntityId());
        }

    }
}
