package com.example.sparkapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class SavedQuotes extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView statsText;
    private List<QuoteIdea> mySavedQuotes = new ArrayList<>();
    private QuoteAdapter adapter;
    private boolean isManageMode = false;
    private int generateCount = 0;
    private int saveCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_quotes);

        recyclerView = findViewById(R.id.savedQuotes);
        statsText = findViewById(R.id.statsText);
        Button manageButton = findViewById(R.id.manageButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new QuoteAdapter(mySavedQuotes, this::deleteSpark);
        recyclerView.setAdapter(adapter);

        loadSparks();
        loadStats();
        updateStats();

        manageButton.setOnClickListener(v -> {
            isManageMode = !isManageMode;
            adapter.setManageMode(isManageMode);
            manageButton.setText(isManageMode ? "完成" : "管理");
            animateButton(v);
        });
    }

    private void deleteSpark(int position) {
        mySavedQuotes.remove(position);
        adapter.updateQuotes(mySavedQuotes);
        saveSparks();
        saveCount = mySavedQuotes.size();
        updateStats();
        Log.d("SparkApp", "Deleted position: " + position);
    }

    private void updateStats() {
        statsText.setText(getString(R.string.stats_format, generateCount, saveCount));
    }

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        generateCount = prefs.getInt("spark_generate_count", 0); // 加载生成次数
        saveCount = mySavedQuotes.size(); // 使用当前保存的灵感数量
    }

    private void saveSparks() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        editor.putString("my_unique_sparks", gson.toJson(mySavedQuotes));
        editor.apply();
    }

    private void loadSparks() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        String jsonString = prefs.getString("my_unique_sparks", "");
        if (!jsonString.isEmpty()) {
            Gson gson = new Gson();
            mySavedQuotes = gson.fromJson(jsonString, new TypeToken<List<QuoteIdea>>(){}.getType());
            adapter.updateQuotes(mySavedQuotes);
        }
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(50).start())
                .start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSparks();
    }
}