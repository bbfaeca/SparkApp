package com.example.sparkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextView quoteText, statsText, fortuneLevel, fortuneGood, fortuneBad, checkInDays;
    private Button checkInButton, manageButton;
    private ConstraintLayout fortuneLayout;
    private List<QuoteIdea> mySavedQuotes = new ArrayList<>();
    private QuoteAdapter adapter;
    private String currentQuote = "";
    private int generateCount = 0;
    private int saveCount = 0;
    private int consecutiveCheckInDays = 0;
    private long lastCheckInDate = 0;
    private boolean isManageMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 UI
        quoteText = findViewById(R.id.quoteText);
        statsText = findViewById(R.id.statsText);
        fortuneLevel = findViewById(R.id.fortuneLevel);
        fortuneGood = findViewById(R.id.fortuneGood);
        fortuneBad = findViewById(R.id.fortuneBad);
        checkInDays = findViewById(R.id.checkInDays);
        checkInButton = findViewById(R.id.checkInButton);
        manageButton = findViewById(R.id.manageButton);
        fortuneLayout = findViewById(R.id.fortuneLayout);
        RecyclerView recyclerView = findViewById(R.id.savedQuotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new QuoteAdapter(mySavedQuotes, this::deleteSpark);
        recyclerView.setAdapter(adapter);

        // 加载数据
        loadSparks();
        loadStats();
        loadCheckInData();

        // 检查是否已签到，并动态调整约束
        Calendar calendar = Calendar.getInstance();
        long currentDate = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
        if (lastCheckInDate == currentDate) {
            checkInButton.setVisibility(View.GONE);
            fortuneLayout.setVisibility(View.VISIBLE);
            updateFortune();
            updateQuoteTextConstraint(true);
        } else {
            checkInButton.setVisibility(View.VISIBLE);
            fortuneLayout.setVisibility(View.GONE);
            updateQuoteTextConstraint(false);
        }

        // 签到按钮
        checkInButton.setOnClickListener(v -> {
            checkIn();
            animateButton(v);
        });

        // 管理按钮
        manageButton.setOnClickListener(v -> {
            isManageMode = !isManageMode;
            adapter.setManageMode(isManageMode);
            manageButton.setText(isManageMode ? "完成" : "管理");
            animateButton(v);
        });

        // 其他按钮
        Button generateButton = findViewById(R.id.generateButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button shareButton = findViewById(R.id.shareButton);

        generateButton.setOnClickListener(v -> {
            createNewSpark();
            animateButton(v);
        });
        saveButton.setOnClickListener(v -> {
            saveSpark();
            animateButton(v);
        });
        shareButton.setOnClickListener(v -> {
            shareSpark();
            animateButton(v);
        });

        updateStats();
    }

    private void updateQuoteTextConstraint(boolean isCheckedIn) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) quoteText.getParent());
        constraintSet.clear(R.id.quoteText, ConstraintSet.TOP);
        if (isCheckedIn) {
            constraintSet.connect(R.id.quoteText, ConstraintSet.TOP, R.id.fortuneLayout, ConstraintSet.BOTTOM, 8);
        } else {
            constraintSet.connect(R.id.quoteText, ConstraintSet.TOP, R.id.checkInButton, ConstraintSet.BOTTOM, 8);
        }
        constraintSet.applyTo((ConstraintLayout) quoteText.getParent());
    }

    private void createNewSpark() {
        String[] quotes = getResources().getStringArray(R.array.quotes);
        currentQuote = quotes[new Random().nextInt(quotes.length)];
        if (System.currentTimeMillis() % 5 == 0) {
            currentQuote = createDynamicSpark();
        }
        quoteText.setText(currentQuote);
        quoteText.setAlpha(0f);
        quoteText.setScaleX(0.8f);
        quoteText.setScaleY(0.8f);
        quoteText.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start();
        generateCount++;
        updateStats();
        saveStats();
        Log.d("Spark", "Generated: " + currentQuote);
    }

    private String createDynamicSpark() {
        String[] prefixes = {"点燃", "探索", "优化", "调试"};
        String prefix = prefixes[(int) (System.currentTimeMillis() % prefixes.length)];
        return prefix + "你的编程灵感！";
    }

    private void checkIn() {
        Calendar calendar = Calendar.getInstance();
        long currentDate = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH);

        // 计算连续签到天数
        if (lastCheckInDate == 0) {
            consecutiveCheckInDays = 1;
        } else {
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(lastCheckInDate * 10000L);
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(currentDate * 10000L);
            long diffDays = (today.getTimeInMillis() - last.getTimeInMillis()) / (1000 * 60 * 60 * 24);
            if (diffDays == 1) {
                consecutiveCheckInDays++;
            } else {
                consecutiveCheckInDays = 1;
            }
        }
        lastCheckInDate = currentDate;
        saveCheckInData();

        // 显示运势
        checkInButton.setVisibility(View.GONE);
        fortuneLayout.setVisibility(View.VISIBLE);
        updateFortune();
        updateQuoteTextConstraint(true);
    }

    private void updateFortune() {
        Calendar calendar = Calendar.getInstance();
        long seed = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
        Random random = new Random(seed);

        // 运势等级
        String[] levels = getResources().getStringArray(R.array.fortune_levels);
        fortuneLevel.setText(levels[random.nextInt(levels.length)]);

        // 宜和忌
        String[] goodActions = getResources().getStringArray(R.array.fortune_good);
        String[] badActions = getResources().getStringArray(R.array.fortune_bad);
        String good1 = goodActions[random.nextInt(goodActions.length)];
        String good2 = goodActions[random.nextInt(goodActions.length)];
        String bad1 = badActions[random.nextInt(badActions.length)];
        String bad2 = badActions[random.nextInt(badActions.length)];
        fortuneGood.setText("宜：" + good1 + "\n" + good2);
        fortuneBad.setText("忌：" + bad1 + "\n" + bad2);

        // 签到天数
        checkInDays.setText(getString(R.string.check_in_days_format, consecutiveCheckInDays));
    }

    private void deleteSpark(int position) {
        mySavedQuotes.remove(position);
        adapter.updateQuotes(mySavedQuotes);
        saveSparks();
        saveCount = mySavedQuotes.size();
        updateStats();
        saveStats();
        Log.d("Spark", "Deleted position: " + position);
    }

    private void saveSpark() {
        if (!currentQuote.isEmpty()) {
            if (mySavedQuotes.size() >= 50) {
                mySavedQuotes.remove(0);
            }
            Random random = new Random();
            int randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            mySavedQuotes.add(new QuoteIdea(currentQuote, randomColor));
            adapter.updateQuotes(mySavedQuotes);
            saveSparks();
            saveCount = mySavedQuotes.size();
            updateStats();
            saveStats();
            Log.d("Spark", "Saved: " + currentQuote + " with color: " + randomColor);
        }
    }

    private void shareSpark() {
        if (!currentQuote.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentQuote);
            startActivity(Intent.createChooser(shareIntent, "分享灵感"));
            Log.d("Spark", "Sharing: " + currentQuote);
        }
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    private void updateStats() {
        statsText.setText(getString(R.string.stats_format, generateCount, saveCount));
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
        String json = prefs.getString("my_unique_sparks", "");
        if (!json.isEmpty()) {
            Gson gson = new Gson();
            mySavedQuotes = gson.fromJson(json, new TypeToken<List<QuoteIdea>>(){}.getType());
            adapter.updateQuotes(mySavedQuotes);
            saveCount = mySavedQuotes.size();
        }
    }

    private void saveStats() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("spark_generate_count", generateCount);
        editor.putInt("spark_save_count", saveCount);
        editor.apply();
    }

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        generateCount = prefs.getInt("spark_generate_count", 0);
        saveCount = prefs.getInt("spark_save_count", 0);
        updateStats();
    }

    private void saveCheckInData() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_check_in_date", lastCheckInDate);
        editor.putInt("consecutive_check_in_days", consecutiveCheckInDays);
        editor.apply();
    }

    private void loadCheckInData() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        lastCheckInDate = prefs.getLong("last_check_in_date", 0);
        consecutiveCheckInDays = prefs.getInt("consecutive_check_in_days", 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSparks();
        saveStats();
        saveCheckInData();
    }
}