package com.example.sparkapp;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView quoteText, statsText, fortuneLevel, fortuneGood, fortuneBad, checkInDays;
    private Button checkInButton, manageButton;
    private ConstraintLayout fortuneLayout;
    private RecyclerView recyclerView;
    private List<QuoteIdea> mySavedQuotes = new ArrayList<>();
    private QuoteAdapter adapter;
    private String currentQuote = "";
    private int generateCount = 0;
    private int saveCount = 0;
    private int consecutiveCheckInDays = 0;
    private long lastCheckInDate = 0;
    private boolean isManageMode = false;

    private static final String APIPASSWORD = "ORBhCzpPCIJWReVOCjhk:msTbNYSXBJlcxHsUOrsS";
    private static final String API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

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
        recyclerView = findViewById(R.id.savedQuotes);

        // 加载签到数据
        loadCheckInData();

        // 临时重置签到数据（调试用，生产环境注释掉）
        resetCheckInData();

        // 检查是否已签到，并动态调整约束
        Calendar calendar = Calendar.getInstance();
        long currentDate = getDateAsLong(calendar);
        Log.d("CheckIn", "Current Date: " + currentDate + ", Last Check-In Date: " + lastCheckInDate);
        if (lastCheckInDate == currentDate) {
            Log.d("CheckIn", "User has already checked in today. Showing fortune layout.");
            checkInButton.setVisibility(View.GONE);
            fortuneLayout.setVisibility(View.VISIBLE);
            updateFortune();
            updateQuoteTextConstraint(true);
        } else {
            Log.d("CheckIn", "User has not checked in today. Showing check-in button.");
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
            new Thread(() -> createNewSpark()).start();
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

    @Override
    protected void onStart() {
        super.onStart();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new QuoteAdapter(mySavedQuotes, this::deleteSpark);
        recyclerView.setAdapter(adapter);

        loadSparks();
        loadStats();
        updateStats();
    }

    private long getDateAsLong(Calendar calendar) {
        long date = calendar.get(Calendar.YEAR) * 10000L + (calendar.get(Calendar.MONTH) + 1) * 100L + calendar.get(Calendar.DAY_OF_MONTH);
        Log.d("CheckIn", "Calculated Date: " + date + " (Year: " + calendar.get(Calendar.YEAR) + ", Month: " + (calendar.get(Calendar.MONTH) + 1) + ", Day: " + calendar.get(Calendar.DAY_OF_MONTH) + ")");
        return date;
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
        String prompt = "生成一个编程相关的灵感，简洁有趣，50字以内,避免生成的有标点符号语句，除了逗号和感叹号";
        callSparkApi(prompt, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    currentQuote = "网络错误，生成失败！";
                    quoteText.setText(currentQuote);
                    Log.e("Spark", "API Call Failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    StringBuilder fullResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String parsed = parseSparkResponse(line);
                        if (!parsed.isEmpty()) {
                            fullResponse.append(parsed);
                        }
                    }
                    reader.close();
                    currentQuote = fullResponse.toString().trim();
                    if (currentQuote.isEmpty()) {
                        currentQuote = "生成失败，未返回内容";
                    }
                    runOnUiThread(() -> {
                        quoteText.setText(currentQuote);
                        quoteText.setAlpha(0f);
                        quoteText.setScaleX(0.8f);
                        quoteText.setScaleY(0.8f);
                        quoteText.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start();
                        generateCount++;
                        updateStats();
                        saveStats();
                        Log.d("Spark", "Generated: " + currentQuote);
                    });
                } else {
                    runOnUiThread(() -> {
                        String errorMsg = "API 错误：" + response.code();
                        currentQuote = errorMsg;
                        quoteText.setText(errorMsg);
                        Log.e("Spark", errorMsg);
                    });
                }
            }
        });
    }

    private void callSparkApi(String prompt, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", "sparkapp_user");
        jsonObject.put("model", "x1");
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messageObject.put("temperature", "0.5");
        messagesArray.put(messageObject);
        jsonObject.put("messages", messagesArray);
        jsonObject.put("stream", true);
        jsonObject.put("max_tokens", 4096);

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        String header = "Authorization: Bearer " + APIPASSWORD;

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", header)
                .build();

        client.newCall(request).enqueue(callback);
    }

    private String parseSparkResponse(String line) {
        if (line.startsWith("data: ")) {
            String jsonStr = line.substring(6);
            if (jsonStr.equals("[DONE]")) {
                return "";
            }
            try {
                JSONObject json = new JSONObject(jsonStr);
                JSONArray choices = json.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject delta = choice.getJSONObject("delta");
                    if (delta != null) {
                        String content = delta.getStr("content", "");
                        return content;
                    }
                }
            } catch (Exception e) {
                Log.e("Spark", "Parse Error: " + e.getMessage());
            }
        }
        return "";
    }

    private void checkIn() {
        Calendar calendar = Calendar.getInstance();
        long currentDate = getDateAsLong(calendar);

        if (lastCheckInDate == 0) {
            consecutiveCheckInDays = 1;
        } else {
            Calendar last = Calendar.getInstance();
            int yearLast = (int) (lastCheckInDate / 10000);
            int monthLast = (int) (((lastCheckInDate % 10000) / 100) - 1);
            int dayLast = (int) (lastCheckInDate % 100);
            last.set(yearLast, monthLast, dayLast);

            Calendar today = Calendar.getInstance();
            int yearToday = (int) (currentDate / 10000);
            int monthToday = (int) (((currentDate % 10000) / 100) - 1);
            int dayToday = (int) (currentDate % 100);
            today.set(yearToday, monthToday, dayToday);

            long diffDays = (today.getTimeInMillis() - last.getTimeInMillis()) / (1000 * 60 * 60 * 24);
            if (diffDays == 1) {
                consecutiveCheckInDays++;
            } else if (diffDays > 1) {
                consecutiveCheckInDays = 1;
            }
        }

        lastCheckInDate = currentDate;
        saveCheckInData();
        Log.d("CheckIn", "After Check-In - Current Date: " + currentDate + ", Last Check-In Date: " + lastCheckInDate + ", Consecutive Days: " + consecutiveCheckInDays);

        checkInButton.setVisibility(View.GONE);
        fortuneLayout.setVisibility(View.VISIBLE);
        updateFortune();
        updateQuoteTextConstraint(true);
    }

    private void updateFortune() {
        // 设置运势等级
        Calendar calendar = Calendar.getInstance();
        long seed = getDateAsLong(calendar);
        final Random random = new Random(seed); // 声明为 final
        String[] levels = getResources().getStringArray(R.array.fortune_levels);
        fortuneLevel.setText(levels[random.nextInt(levels.length)]);

        // 使用 AI 生成“宜”和“忌”
        new Thread(() -> {
            // 生成“宜”的内容
            String goodPrompt = "生成两条适合程序员的今日宜做事项，每条不超过 10 字，逗号分隔，事项必须不同，例如：写代码,调试程序";
            callSparkApi(goodPrompt, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        String[] goodActions = getResources().getStringArray(R.array.fortune_good);
                        String good1 = goodActions[random.nextInt(goodActions.length)];
                        String good2 = goodActions[random.nextInt(goodActions.length)];
                        while (good1.equals(good2)) {
                            good2 = goodActions[random.nextInt(goodActions.length)]; // 内部变量，无需 final
                        }
                        fortuneGood.setText("宜：" + good1 + "\n宜：" + good2);
                        Log.e("Fortune", "API Failed for Good Actions: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                        StringBuilder fullResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String parsed = parseSparkResponse(line);
                            if (!parsed.isEmpty()) {
                                fullResponse.append(parsed);
                            }
                        }
                        reader.close();
                        String result = fullResponse.toString().trim();
                        Log.d("Fortune", "Good Actions Result: " + result);
                        if (result.isEmpty()) {
                            result = "写代码,调试程序";
                        }
                        String[] goodActions = result.split(",");
                        if (goodActions.length < 2) {
                            result = "写代码,调试程序";
                            goodActions = result.split(",");
                        }
                        final String good1 = goodActions[0].trim(); // 声明为 final
                        final String good2 = goodActions.length > 1 ? goodActions[1].trim() : "学习新技能";
                        runOnUiThread(() -> {
                            fortuneGood.setText("宜：" + good1 + "\n宜：" + good2);
                        });
                    } else {
                        runOnUiThread(() -> {
                            String[] goodActions = getResources().getStringArray(R.array.fortune_good);
                            String good1 = goodActions[random.nextInt(goodActions.length)];
                            String good2 = goodActions[random.nextInt(goodActions.length)];
                            while (good1.equals(good2)) {
                                good2 = goodActions[random.nextInt(goodActions.length)];
                            }
                            fortuneGood.setText("宜：" + good1 + "\n宜：" + good2);
                            Log.e("Fortune", "API Error for Good Actions: " + response.code());
                        });
                    }
                }
            });

            // 生成“忌”的内容
            String badPrompt = "生成两条适合程序员的今日忌做事项，每条不超过 10 字，逗号分隔，事项必须不同，例如：熬夜编码,忽视测试";
            callSparkApi(badPrompt, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        String[] badActions = getResources().getStringArray(R.array.fortune_bad);
                        String bad1 = badActions[random.nextInt(badActions.length)];
                        String bad2 = badActions[random.nextInt(badActions.length)];
                        while (bad1.equals(bad2)) {
                            bad2 = badActions[random.nextInt(badActions.length)];
                        }
                        fortuneBad.setText("忌：" + bad1 + "\n忌：" + bad2);
                        Log.e("Fortune", "API Failed for Bad Actions: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                        StringBuilder fullResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String parsed = parseSparkResponse(line);
                            if (!parsed.isEmpty()) {
                                fullResponse.append(parsed);
                            }
                        }
                        reader.close();
                        String result = fullResponse.toString().trim();
                        Log.d("Fortune", "Bad Actions Result: " + result);
                        if (result.isEmpty()) {
                            result = "熬夜编码,忽视测试";
                        }
                        String[] badActions = result.split(",");
                        if (badActions.length < 2) {
                            result = "熬夜编码,忽视测试";
                            badActions = result.split(",");
                        }
                        final String bad1 = badActions[0].trim(); // 声明为 final
                        final String bad2 = badActions.length > 1 ? badActions[1].trim() : "拖延任务";
                        runOnUiThread(() -> {
                            fortuneBad.setText("忌：" + bad1 + "\n忌：" + bad2);
                        });
                    } else {
                        runOnUiThread(() -> {
                            String[] badActions = getResources().getStringArray(R.array.fortune_bad);
                            String bad1 = badActions[random.nextInt(badActions.length)];
                            String bad2 = badActions[random.nextInt(badActions.length)];
                            while (bad1.equals(bad2)) {
                                bad2 = badActions[random.nextInt(badActions.length)];
                            }
                            fortuneBad.setText("忌：" + bad1 + "\n忌：" + bad2);
                            Log.e("Fortune", "API Error for Bad Actions: " + response.code());
                        });
                    }
                }
            });
        }).start();

        // 更新签到天数
        checkInDays.setText(getString(R.string.check_in_days_format, consecutiveCheckInDays));
    }

    private void deleteSpark(int position) {
        mySavedQuotes.remove(position);
        adapter.updateQuotes(mySavedQuotes);
        saveSparks();
        saveCount = mySavedQuotes.size();
        updateStats();
        saveStats();
        Log.d("SparkApp", "Deleted position: " + position);
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
            Log.d("SparkApp", "Saved: " + currentQuote + " with color: " + randomColor);
        }
    }

    private void shareSpark() {
        if (!currentQuote.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentQuote);
            startActivity(Intent.createChooser(shareIntent, "分享灵感"));
            Log.d("SparkApp", "Shared: " + currentQuote);
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
        String jsonString = prefs.getString("my_unique_sparks", "");
        if (!jsonString.isEmpty()) {
            Gson gson = new Gson();
            mySavedQuotes = gson.fromJson(jsonString, new TypeToken<List<QuoteIdea>>(){}.getType());
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
        Log.d("CheckIn", "Saved Check-In Data - Last Check-In Date: " + lastCheckInDate + ", Consecutive Days: " + consecutiveCheckInDays);
    }

    private void loadCheckInData() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        lastCheckInDate = prefs.getLong("last_check_in_date", 0);
        consecutiveCheckInDays = prefs.getInt("consecutive_check_in_days", 0);
        Log.d("CheckIn", "Loaded Check-In Data - Last Check-In Date: " + lastCheckInDate + ", Consecutive Days: " + consecutiveCheckInDays);
    }

    private void resetCheckInData() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_check_in_date", 0);
        editor.putInt("consecutive_check_in_days", 0);
        editor.apply();
        lastCheckInDate = 0;
        consecutiveCheckInDays = 0;
        Log.d("CheckIn", "Reset Check-In Data - Last Check-In Date: " + lastCheckInDate + ", Consecutive Days: " + consecutiveCheckInDays);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Log.e("CrashHandler", "Uncaught Exception: " + ex.getMessage(), ex);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSparks();
        saveStats();
        saveCheckInData();
    }
}