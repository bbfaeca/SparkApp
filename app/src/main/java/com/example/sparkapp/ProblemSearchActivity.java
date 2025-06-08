package com.example.sparkapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.graphics.Color;

public class ProblemSearchActivity extends AppCompatActivity {
    private LinearLayout searchResults;
    private EditText searchInput;
    private List<FavoriteProblemsActivity.Problem> favorites = new ArrayList<>();
    private List<MainActivity.ChallengeProblem> dailyChallenges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_search);

        searchResults = findViewById(R.id.searchResults);
        searchInput = findViewById(R.id.searchInput);

        // 加载收藏和每日挑战历史
        loadFavorites();
        loadDailyChallengesHistory();

        // 监听搜索输入
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchProblems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDailyChallengesHistory() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        String jsonString = prefs.getString(MainActivity.DAILY_CHALLENGES_KEY, "[]");
        Gson gson = new Gson();
        List<MainActivity.ChallengeProblem> tempChallenges = gson.fromJson(jsonString, new TypeToken<List<MainActivity.ChallengeProblem>>(){}.getType());
        if (tempChallenges == null) tempChallenges = new ArrayList<>();

        // 去重逻辑：基于 pid
        Set<String> uniquePids = new HashSet<>();
        dailyChallenges.clear();
        for (MainActivity.ChallengeProblem problem : tempChallenges) {
            if (uniquePids.add(problem.getPid())) { // add 返回 false 表示重复
                dailyChallenges.add(problem);
            } else {
                Log.w("ProblemSearch", "Duplicate pid found and removed: " + problem.getPid());
            }
        }
        Log.d("ProblemSearch", "Loaded Daily Challenges History (after deduplication): " + gson.toJson(dailyChallenges));
    }

    private void searchProblems(String query) {
        searchResults.removeAllViews();
        if (query.isEmpty()) return;

        String lowerQuery = query.toLowerCase();
        Set<String> addedPids = new HashSet<>(); // 防止搜索结果中重复渲染
        for (MainActivity.ChallengeProblem problem : dailyChallenges) {
            String pid = problem.getPid().toLowerCase();
            String title = problem.getTitle().toLowerCase().replaceAll("\\[.*?\\]", "").trim();
            if ((pid.contains(lowerQuery) || title.contains(lowerQuery)) && addedPids.add(problem.getPid())) {
                View itemView = getLayoutInflater().inflate(R.layout.item_problem, searchResults, false);

                TextView problemText = itemView.findViewById(R.id.problemText);
                problemText.setText(problem.getPid() + " " + problem.getTitle() + " (" + problem.getDifficulty() + ")");

                Button starButton = itemView.findViewById(R.id.starButton);
                starButton.setTag(problem.getPid());
                starButton.setTextColor(isFavorite(problem.getPid()) ? Color.YELLOW : Color.WHITE);
                int finalIndex = dailyChallenges.indexOf(problem);
                starButton.setOnClickListener(v -> toggleFavorite(starButton, problem.getPid(), problem.getTitle(), problem.getDifficulty()));

                searchResults.addView(itemView);
                Log.d("ProblemSearch", "Added search result: " + problem.getPid() + " - " + problem.getTitle());
            } else if (!addedPids.add(problem.getPid())) {
                Log.w("ProblemSearch", "Skipped duplicate pid in search results: " + problem.getPid());
            }
        }
    }

    private void toggleFavorite(Button starButton, String pid, String title, String difficulty) {
        boolean isCurrentlyFavorite = isFavorite(pid);
        if (isCurrentlyFavorite) {
            removeFromFavorites(pid);
            starButton.setTextColor(Color.WHITE);
        } else {
            addToFavorites(pid, title, difficulty);
            starButton.setTextColor(Color.YELLOW);
        }
        starButton.setEnabled(true);
    }

    private boolean isFavorite(String pid) {
        loadFavorites();
        for (FavoriteProblemsActivity.Problem problem : favorites) {
            if (problem.getPid().equals(pid)) return true;
        }
        return false;
    }

    private void addToFavorites(String pid, String title, String difficulty) {
        FavoriteProblemsActivity.Problem problem = new FavoriteProblemsActivity.Problem(pid, title, difficulty);
        if (!favorites.contains(problem)) {
            favorites.add(problem);
            saveFavorites();
        }
    }

    private void removeFromFavorites(String pid) {
        favorites.removeIf(problem -> problem.getPid().equals(pid));
        saveFavorites();
    }

    private void loadFavorites() {
        SharedPreferences prefs = getSharedPreferences("SparkApp2025", MODE_PRIVATE);
        String jsonString = prefs.getString("favorite_problems", "[]");
        Gson gson = new Gson();
        favorites = gson.fromJson(jsonString, new TypeToken<List<FavoriteProblemsActivity.Problem>>(){}.getType());
        if (favorites == null) favorites = new ArrayList<>();
    }

    private void saveFavorites() {
        Gson gson = new Gson();
        String jsonString = gson.toJson(favorites);
        getSharedPreferences("SparkApp2025", MODE_PRIVATE).edit()
                .putString("favorite_problems", jsonString)
                .apply();
    }
}