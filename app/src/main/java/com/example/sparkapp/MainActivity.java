package com.example.sparkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private TextView quoteText, fortuneLevel, fortuneGood, fortuneBad, checkInDays;
    private Button checkInButton, viewSavedQuotesButton, viewFavoritesButton;
    private ConstraintLayout fortuneLayout;
    private LinearLayout buttonLayout;
    private List<QuoteIdea> mySavedQuotes = new ArrayList<>();
    private String currentQuote = "";
    private int consecutiveCheckInDays = 0;
    private long lastCheckInDate = 0;

    // 每日挑战相关视图
    private TextView challengeTitle, challengeItem1, challengeItem2, challengeItem3, challengeItem4;
    private Button starButton1, starButton2, starButton3, starButton4;
    private LinearLayout moreButtonLayout;

    private static final String LUOGU_API_URL = "https://www.luogu.com.cn/problem/list"; // 洛谷题目列表端点
    private final OkHttpClient client = new OkHttpClient();

    // 每日挑战历史数据
    public static final String DAILY_CHALLENGES_KEY = "daily_challenges_history";
    private static final int DAYS_TO_KEEP = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 UI
        quoteText = findViewById(R.id.quoteText);
        fortuneLevel = findViewById(R.id.fortuneLevel);
        fortuneGood = findViewById(R.id.fortuneGood);
        fortuneBad = findViewById(R.id.fortuneBad);
        checkInDays = findViewById(R.id.checkInDays);
        checkInButton = findViewById(R.id.checkInButton);
        viewSavedQuotesButton = findViewById(R.id.viewSavedQuotesButton);
        viewFavoritesButton = findViewById(R.id.viewFavoritesButton);
        fortuneLayout = findViewById(R.id.fortuneLayout);
        buttonLayout = findViewById(R.id.buttonLayout);

        // 初始化每日挑战相关视图
        challengeTitle = findViewById(R.id.challengeTitle);
        challengeItem1 = findViewById(R.id.challengeItem1);
        challengeItem2 = findViewById(R.id.challengeItem2);
        challengeItem3 = findViewById(R.id.challengeItem3);
        challengeItem4 = findViewById(R.id.challengeItem4);
        starButton1 = findViewById(R.id.starButton1);
        starButton2 = findViewById(R.id.starButton2);
        starButton3 = findViewById(R.id.starButton3);
        starButton4 = findViewById(R.id.starButton4);
        moreButtonLayout = findViewById(R.id.moreButtonLayout);

        // 加载签到数据
        loadCheckInData();

        // 临时重置签到数据（调试用，生产环境注释掉）
        resetCheckInData();

        // 检查是否已签到
        Calendar calendar = Calendar.getInstance();
        long currentDate = getDateAsLong(calendar);
        Log.d("CheckIn", "Current Date: " + currentDate + ", Last Check-In Date: " + lastCheckInDate);
        if (lastCheckInDate == currentDate) {
            Log.d("CheckIn", "User has already checked in today. Showing fortune layout.");
            checkInButton.setVisibility(View.GONE);
            fortuneLayout.setVisibility(View.VISIBLE);
            updateFortune();
        } else {
            Log.d("CheckIn", "User has not checked in today. Showing check-in button.");
            checkInButton.setVisibility(View.VISIBLE);
            fortuneLayout.setVisibility(View.GONE);
        }

        // 签到按钮
        checkInButton.setOnClickListener(v -> {
            checkIn();
            animateButton(v);
        });

        // 查看保存的灵感按钮
        viewSavedQuotesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedQuotes.class);
            startActivity(intent);
            animateButton(v);
        });

        // 查看收藏按钮
        viewFavoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoriteProblemsActivity.class);
            startActivity(intent);
            animateButton(v);
        });

        // 其他按钮
        Button generateButton = findViewById(R.id.generateButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button shareButton = findViewById(R.id.shareButton);

        generateButton.setOnClickListener(v -> {
            new Thread(() -> createNewIdea()).start();
            animateButton(v);
        });
        saveButton.setOnClickListener(v -> {
            saveIdea();
            animateButton(v);
        });
        shareButton.setOnClickListener(v -> {
            shareIdea();
            animateButton(v);
        });

        // 更多按钮点击事件
        moreButtonLayout.setOnClickListener(v -> {
            Log.d("MainActivity", "More button clicked");
            Intent intent = new Intent(MainActivity.this, ProblemSearchActivity.class);
            startActivity(intent);
            animateButton(v);
        });

        // 为每日挑战题目添加点击事件和五角星按钮事件
        setupChallengeClickListeners();

        // 初始化每日挑战
        updateDailyChallenges();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadIdeas();
    }

    private void updateButtonLayoutConstraint(boolean isCheckedIn) {
        // 不再需要动态调整约束，因为 quoteText 始终可见
    }

    private static long getDateAsLong(Calendar calendar) {
        long date = calendar.get(Calendar.YEAR) * 10000L + (calendar.get(Calendar.MONTH) + 1) * 100L + calendar.get(Calendar.DAY_OF_MONTH);
        Log.d("CheckIn", "Calculated Date: " + date + " (Year: " + calendar.get(Calendar.YEAR) + ", Month: " + (calendar.get(Calendar.MONTH) + 1) + ", Day: " + calendar.get(Calendar.DAY_OF_MONTH) + ")");
        return date;
    }

    private void createNewIdea() {
        String prompt = "生成一个编程相关的灵感，简洁有趣，严格保证在20字以内,避免生成除了逗号和感叹号的有标点符号语句,也可以用空格隔开，尽量避免产生有换行的语句";
        callPpioApi(prompt, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    currentQuote = "网络错误，生成失败！";
                    quoteText.setText(currentQuote);
                    Log.e("Qwen", "API Call Failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    StringBuilder fullResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String parsed = parsePpioResponse(line);
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
                        Log.d("Qwen", "Generated: " + currentQuote);
                        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
                        generateCount = prefs.getInt("spark_generate_count", 0) + 1;
                        prefs.edit().putInt("spark_generate_count", generateCount).apply();
                    });
                } else {
                    runOnUiThread(() -> {
                        String errorMsg = "API 错误：" + response.code();
                        currentQuote = errorMsg;
                        quoteText.setText(errorMsg);
                        Log.e("Qwen", errorMsg);
                    });
                }
            }
        });
    }
    private int generateCount = 0;

    private void callPpioApi(String prompt, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "qwen/qwen2.5-7b-instruct");
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject().put("role", "system").put("content", "您是一个专业的 AI 助手，诚实且用中文回答问题。"));
        messagesArray.put(new JSONObject().put("role", "user").put("content", prompt));
        jsonObject.put("messages", messagesArray);
        jsonObject.put("stream", true);
        jsonObject.put("max_tokens", 32000);
        jsonObject.put("temperature", 1);
        jsonObject.put("top_p", 1);
        jsonObject.put("top_k", 50);
        jsonObject.put("repetition_penalty", 1);
        jsonObject.put("response_format", new JSONObject().put("type", "text"));

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        // PPIO API 密钥
        String apiKey = "sk_6WVjXH0nJLf8fP2q9qgYzXjRs_2ASpVd_s47ZS9OFHw";
        Request request = new Request.Builder()
                .url("https://api.ppinfra.com/v3/openai/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        client.newCall(request).enqueue(callback);
    }

    private String parsePpioResponse(String line) {
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
                Log.e("Qwen", "Parse Error: " + e.getMessage());
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
    }

    private void updateFortune() {
        Calendar calendar = Calendar.getInstance();
        long seed = getDateAsLong(calendar);
        final Random random = new Random(seed);
        String[] levels = getResources().getStringArray(R.array.fortune_levels);
        fortuneLevel.setText(levels[random.nextInt(levels.length)]);

        new Thread(() -> {
            String goodPrompt = "生成两条适合程序员的今日宜做事项，每条不超过 10 字，逗号分隔，事项必须不同，例如：写代码,调试程序";
            callPpioApi(goodPrompt, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        String[] goodActions = getResources().getStringArray(R.array.fortune_good);
                        String good1 = goodActions[random.nextInt(goodActions.length)];
                        String good2 = goodActions[random.nextInt(goodActions.length)];
                        while (good1.equals(good2)) {
                            good2 = goodActions[random.nextInt(goodActions.length)];
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
                            String parsed = parsePpioResponse(line);
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
                        final String good1 = goodActions[0].trim();
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

            String badPrompt = "生成两条适合程序员的今日忌做事项，每条不超过 10 字，逗号分隔，事项必须不同，例如：熬夜编码,忽视测试";
            callPpioApi(badPrompt, new Callback() {
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
                            String parsed = parsePpioResponse(line);
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
                        final String bad1 = badActions[0].trim();
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

        checkInDays.setText(getString(R.string.check_in_days_format, consecutiveCheckInDays));
    }

    private void saveIdea() {
        if (!currentQuote.isEmpty()) {
            if (mySavedQuotes.size() >= 50) {
                mySavedQuotes.remove(0);
            }
            Random random = new Random();
            int randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            mySavedQuotes.add(new QuoteIdea(currentQuote, randomColor));
            saveIdeas();
            Log.d("IdeaApp", "Saved: " + currentQuote + " with color: " + randomColor);
        }
    }

    private void shareIdea() {
        if (!currentQuote.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentQuote);
            startActivity(Intent.createChooser(shareIntent, "分享灵感"));
            Log.d("IdeaApp", "Shared: " + currentQuote);
        }
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    private void saveIdeas() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        editor.putString("my_unique_sparks", gson.toJson(mySavedQuotes));
        editor.apply();
    }

    private void loadIdeas() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        String jsonString = prefs.getString("my_unique_sparks", "");
        if (!jsonString.isEmpty()) {
            Gson gson = new Gson();
            mySavedQuotes = gson.fromJson(jsonString, new TypeToken<List<QuoteIdea>>(){}.getType());
        }
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
        saveIdeas();
        saveCheckInData();
    }

    private void updateDailyChallenges() {
        String url = LUOGU_API_URL + "?type=P&keyword=入门"; // 入门级题目作为每日挑战
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-luogu-type", "content-only")
                .addHeader("referer", "https://www.luogu.com.cn/")
                .addHeader("user-agent", "SparkApp/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("DailyChallenge", "API Call Failed: " + e.getMessage());
                    challengeItem1.setText("加载失败: " + e.getMessage());
                    challengeItem2.setText("加载失败: " + e.getMessage());
                    challengeItem3.setText("加载失败: " + e.getMessage());
                    challengeItem4.setText("加载失败: " + e.getMessage());
                    starButton1.setVisibility(View.GONE);
                    starButton2.setVisibility(View.GONE);
                    starButton3.setVisibility(View.GONE);
                    starButton4.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body == null) {
                            runOnUiThread(() -> {
                                String errorMsg = "API 响应体为空";
                                Log.e("DailyChallenge", errorMsg);
                                challengeItem1.setText(errorMsg);
                                challengeItem2.setText(errorMsg);
                                challengeItem3.setText(errorMsg);
                                challengeItem4.setText(errorMsg);
                                starButton1.setVisibility(View.GONE);
                                starButton2.setVisibility(View.GONE);
                                starButton3.setVisibility(View.GONE);
                                starButton4.setVisibility(View.GONE);
                            });
                            return;
                        }
                        BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                        StringBuilder fullResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            fullResponse.append(line);
                        }
                        String result = fullResponse.toString().trim();
                        Log.d("DailyChallenge", "API Response: " + result);
                        try {
                            JSONObject json = new JSONObject(result);
                            JSONObject currentData = json.getJSONObject("currentData");
                            if (currentData != null) {
                                JSONObject problems = currentData.getJSONObject("problems");
                                if (problems != null) {
                                    JSONArray problemList = problems.getJSONArray("result");
                                    if (problemList != null && problemList.size() >= 4) {
                                        List<ChallengeProblem> dailyChallenges = new ArrayList<>();
                                        dailyChallenges.add(new ChallengeProblem(problemList.getJSONObject(0).getStr("pid", "P0000"), problemList.getJSONObject(0).getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim(), getDifficulty(problemList.getJSONObject(0).getInt("difficulty", 1))));
                                        dailyChallenges.add(new ChallengeProblem(problemList.getJSONObject(1).getStr("pid", "P0000"), problemList.getJSONObject(1).getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim(), getDifficulty(problemList.getJSONObject(1).getInt("difficulty", 1))));
                                        dailyChallenges.add(new ChallengeProblem(problemList.getJSONObject(2).getStr("pid", "P0000"), problemList.getJSONObject(2).getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim(), getDifficulty(problemList.getJSONObject(2).getInt("difficulty", 1))));
                                        dailyChallenges.add(new ChallengeProblem(problemList.getJSONObject(3).getStr("pid", "P0000"), problemList.getJSONObject(3).getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim(), getDifficulty(problemList.getJSONObject(3).getInt("difficulty", 1))));

                                        // 保存当天的挑战
                                        saveDailyChallenges(dailyChallenges);

                                        runOnUiThread(() -> {
                                            challengeItem1.setText(dailyChallenges.get(0).getPid() + " " + dailyChallenges.get(0).getTitle() + " (" + dailyChallenges.get(0).getDifficulty() + ")");
                                            challengeItem1.setTag(problemList.getJSONObject(0));
                                            starButton1.setTag(dailyChallenges.get(0).getPid());
                                            starButton1.setVisibility(View.VISIBLE);
                                            starButton1.setTextColor(isFavorite(dailyChallenges.get(0).getPid()) ? Color.YELLOW : Color.WHITE);

                                            challengeItem2.setText(dailyChallenges.get(1).getPid() + " " + dailyChallenges.get(1).getTitle() + " (" + dailyChallenges.get(1).getDifficulty() + ")");
                                            challengeItem2.setTag(problemList.getJSONObject(1));
                                            starButton2.setTag(dailyChallenges.get(1).getPid());
                                            starButton2.setVisibility(View.VISIBLE);
                                            starButton2.setTextColor(isFavorite(dailyChallenges.get(1).getPid()) ? Color.YELLOW : Color.WHITE);

                                            challengeItem3.setText(dailyChallenges.get(2).getPid() + " " + dailyChallenges.get(2).getTitle() + " (" + dailyChallenges.get(2).getDifficulty() + ")");
                                            challengeItem3.setTag(problemList.getJSONObject(2));
                                            starButton3.setTag(dailyChallenges.get(2).getPid());
                                            starButton3.setVisibility(View.VISIBLE);
                                            starButton3.setTextColor(isFavorite(dailyChallenges.get(2).getPid()) ? Color.YELLOW : Color.WHITE);

                                            challengeItem4.setText(dailyChallenges.get(3).getPid() + " " + dailyChallenges.get(3).getTitle() + " (" + dailyChallenges.get(3).getDifficulty() + ")");
                                            challengeItem4.setTag(problemList.getJSONObject(3));
                                            starButton4.setTag(dailyChallenges.get(3).getPid());
                                            starButton4.setVisibility(View.VISIBLE);
                                            starButton4.setTextColor(isFavorite(dailyChallenges.get(3).getPid()) ? Color.YELLOW : Color.WHITE);
                                        });
                                    } else {
                                        runOnUiThread(() -> {
                                            challengeItem1.setText("数据不足");
                                            challengeItem2.setText("数据不足");
                                            challengeItem3.setText("数据不足");
                                            challengeItem4.setText("数据不足");
                                            starButton1.setVisibility(View.GONE);
                                            starButton2.setVisibility(View.GONE);
                                            starButton3.setVisibility(View.GONE);
                                            starButton4.setVisibility(View.GONE);
                                        });
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        challengeItem1.setText("问题数据为空");
                                        challengeItem2.setText("问题数据为空");
                                        challengeItem3.setText("问题数据为空");
                                        challengeItem4.setText("问题数据为空");
                                        starButton1.setVisibility(View.GONE);
                                        starButton2.setVisibility(View.GONE);
                                        starButton3.setVisibility(View.GONE);
                                        starButton4.setVisibility(View.GONE);
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    challengeItem1.setText("当前数据为空");
                                    challengeItem2.setText("当前数据为空");
                                    challengeItem3.setText("当前数据为空");
                                    challengeItem4.setText("当前数据为空");
                                    starButton1.setVisibility(View.GONE);
                                    starButton2.setVisibility(View.GONE);
                                    starButton3.setVisibility(View.GONE);
                                    starButton4.setVisibility(View.GONE);
                                });
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Log.e("DailyChallenge", "Parse Error: " + e.getMessage() + ", Response: " + result);
                                challengeItem1.setText("解析失败: " + e.getMessage());
                                challengeItem2.setText("解析失败: " + e.getMessage());
                                challengeItem3.setText("解析失败: " + e.getMessage());
                                challengeItem4.setText("解析失败: " + e.getMessage());
                                starButton1.setVisibility(View.GONE);
                                starButton2.setVisibility(View.GONE);
                                starButton3.setVisibility(View.GONE);
                                starButton4.setVisibility(View.GONE);
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            Log.e("DailyChallenge", "I/O Error: " + e.getMessage());
                            challengeItem1.setText("I/O 错误: " + e.getMessage());
                            challengeItem2.setText("I/O 错误: " + e.getMessage());
                            challengeItem3.setText("I/O 错误: " + e.getMessage());
                            challengeItem4.setText("I/O 错误: " + e.getMessage());
                            starButton1.setVisibility(View.GONE);
                            starButton2.setVisibility(View.GONE);
                            starButton3.setVisibility(View.GONE);
                            starButton4.setVisibility(View.GONE);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        try {
                            String errorMsg = "API 错误：" + response.code() + " - " + response.message();
                            Log.e("DailyChallenge", errorMsg + ", Response: " + (response.body() != null ? response.body().string() : "无响应体"));
                            challengeItem1.setText(errorMsg);
                            challengeItem2.setText(errorMsg);
                            challengeItem3.setText(errorMsg);
                            challengeItem4.setText(errorMsg);
                            starButton1.setVisibility(View.GONE);
                            starButton2.setVisibility(View.GONE);
                            starButton3.setVisibility(View.GONE);
                            starButton4.setVisibility(View.GONE);
                        } catch (IOException e) {
                            Log.e("DailyChallenge", "读取错误响应失败: " + e.getMessage());
                            challengeItem1.setText("读取错误失败");
                            challengeItem2.setText("读取错误失败");
                            challengeItem3.setText("读取错误失败");
                            challengeItem4.setText("读取错误失败");
                            starButton1.setVisibility(View.GONE);
                            starButton2.setVisibility(View.GONE);
                            starButton3.setVisibility(View.GONE);
                            starButton4.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void setupChallengeClickListeners() {
        // 题目点击事件
        challengeItem1.setOnClickListener(v -> {
            JSONObject item = (JSONObject) v.getTag();
            if (item != null) {
                String pid = item.getStr("pid", "P0000");
                String title = item.getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim();
                Intent intent = new Intent(MainActivity.this, ProblemDetailActivity.class);
                intent.putExtra("pid", pid);
                intent.putExtra("title", title);
                startActivity(intent);
            }
        });

        challengeItem2.setOnClickListener(v -> {
            JSONObject item = (JSONObject) v.getTag();
            if (item != null) {
                String pid = item.getStr("pid", "P0000");
                String title = item.getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim();
                Intent intent = new Intent(MainActivity.this, ProblemDetailActivity.class);
                intent.putExtra("pid", pid);
                intent.putExtra("title", title);
                startActivity(intent);
            }
        });

        challengeItem3.setOnClickListener(v -> {
            JSONObject item = (JSONObject) v.getTag();
            if (item != null) {
                String pid = item.getStr("pid", "P0000");
                String title = item.getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim();
                Intent intent = new Intent(MainActivity.this, ProblemDetailActivity.class);
                intent.putExtra("pid", pid);
                intent.putExtra("title", title);
                startActivity(intent);
            }
        });

        challengeItem4.setOnClickListener(v -> {
            JSONObject item = (JSONObject) v.getTag();
            if (item != null) {
                String pid = item.getStr("pid", "P0000");
                String title = item.getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim();
                Intent intent = new Intent(MainActivity.this, ProblemDetailActivity.class);
                intent.putExtra("pid", pid);
                intent.putExtra("title", title);
                startActivity(intent);
            }
        });

        // 五角星按钮点击事件
        starButton1.setOnClickListener(v -> toggleFavorite(starButton1));
        starButton2.setOnClickListener(v -> toggleFavorite(starButton2));
        starButton3.setOnClickListener(v -> toggleFavorite(starButton3));
        starButton4.setOnClickListener(v -> toggleFavorite(starButton4));
    }

    private void toggleFavorite(Button starButton) {
        String pid = (String) starButton.getTag();
        if (pid != null) {
            boolean isCurrentlyFavorite = isFavorite(pid);
            if (isCurrentlyFavorite) {
                removeFromFavorites(pid);
                starButton.setTextColor(Color.WHITE);
            } else {
                JSONObject item = (JSONObject) challengeItem1.getTag();
                if (item != null) {
                    addToFavorites(pid, item.getStr("title", "加载失败").replaceAll("\\[.*?\\]", "").trim(), getDifficulty(item.getInt("difficulty", 1)));
                    starButton.setTextColor(Color.YELLOW);
                }
            }
            starButton.setEnabled(true);
        }
    }

    private boolean isFavorite(String pid) {
        List<FavoriteProblemsActivity.Problem> favorites = loadFavorites();
        for (FavoriteProblemsActivity.Problem problem : favorites) {
            if (problem.getPid().equals(pid)) {
                return true;
            }
        }
        return false;
    }

    private void addToFavorites(String pid, String title, String difficulty) {
        FavoriteProblemsActivity.Problem problem = new FavoriteProblemsActivity.Problem(pid, title, difficulty);
        List<FavoriteProblemsActivity.Problem> favorites = loadFavorites();
        if (!favorites.contains(problem)) {
            favorites.add(problem);
            saveFavorites(favorites);
        }
    }

    private void removeFromFavorites(String pid) {
        List<FavoriteProblemsActivity.Problem> favorites = loadFavorites();
        favorites.removeIf(problem -> problem.getPid().equals(pid));
        saveFavorites(favorites);
    }

    private List<FavoriteProblemsActivity.Problem> loadFavorites() {
        String jsonString = getSharedPreferences("SparkApp2025", MODE_PRIVATE)
                .getString("favorite_problems", "[]");
        Gson gson = new Gson();
        List<FavoriteProblemsActivity.Problem> favorites = gson.fromJson(jsonString, new TypeToken<List<FavoriteProblemsActivity.Problem>>(){}.getType());
        return favorites != null ? favorites : new ArrayList<>();
    }

    private void saveFavorites(List<FavoriteProblemsActivity.Problem> favorites) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(favorites);
        getSharedPreferences("SparkApp2025", MODE_PRIVATE).edit()
                .putString("favorite_problems", jsonString)
                .apply();
    }

    private String getDifficulty(int difficulty) {
        switch (difficulty) {
            case 1: return "入门";
            case 2: return "普及−";
            case 3: return "普及/提高−";
            case 4: return "普及+/提高";
            case 5: return "提高+/省选−";
            case 6: return "省选/NOI−";
            default: return "NOI/NOI+/CTSC";
        }
    }

    // 每日挑战数据类
    static class ChallengeProblem {
        private String pid;
        private String title;
        private String difficulty;
        private long date; // 添加日期以跟踪

        public ChallengeProblem(String pid, String title, String difficulty) {
            this.pid = pid;
            this.title = title;
            this.difficulty = difficulty;
            this.date = getDateAsLong(Calendar.getInstance());
        }

        public String getPid() { return pid; }
        public String getTitle() { return title; }
        public String getDifficulty() { return difficulty; }
        public long getDate() { return date; }
    }

    private void saveDailyChallenges(List<ChallengeProblem> challenges) {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonString = prefs.getString(DAILY_CHALLENGES_KEY, "[]");
        List<ChallengeProblem> history = gson.fromJson(jsonString, new TypeToken<List<ChallengeProblem>>(){}.getType());
        if (history == null) history = new ArrayList<>();

        // 去重
        Set<String> uniquePids = new HashSet<>();
        List<ChallengeProblem> deduplicatedHistory = new ArrayList<>();
        for (ChallengeProblem problem : history) {
            if (uniquePids.add(problem.getPid())) {
                deduplicatedHistory.add(problem);
            }
        }
        for (ChallengeProblem problem : challenges) {
            if (uniquePids.add(problem.getPid())) {
                deduplicatedHistory.add(problem);
            }
        }

        // 移除超过 7 天的记录
        deduplicatedHistory.removeIf(problem -> getDateAsLong(Calendar.getInstance()) - problem.getDate() > DAYS_TO_KEEP * 10000L);

        prefs.edit().putString(DAILY_CHALLENGES_KEY, gson.toJson(deduplicatedHistory)).apply();
        Log.d("DailyChallenge", "Saved Daily Challenges (deduplicated): " + gson.toJson(deduplicatedHistory));
    }
}