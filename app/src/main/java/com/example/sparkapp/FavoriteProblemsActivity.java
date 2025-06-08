package com.example.sparkapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;

public class FavoriteProblemsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProblemAdapter adapter;
    private List<Problem> favorites = new ArrayList<>();

    static class Problem {
        private String pid;
        private String title;
        private String difficulty;

        public Problem(String pid, String title, String difficulty) {
            this.pid = pid;
            this.title = title;
            this.difficulty = difficulty;
        }

        public String getPid() { return pid; }
        public String getTitle() { return title; }
        public String getDifficulty() { return difficulty; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Problem other = (Problem) obj;
            return pid.equals(other.pid);
        }

        @Override
        public int hashCode() {
            return pid.hashCode();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_problems);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProblemAdapter();
        recyclerView.setAdapter(adapter);

        loadFavorites();
        adapter.setProblems(favorites);
    }

    private void loadFavorites() {
        String jsonString = getSharedPreferences("SparkApp2025", MODE_PRIVATE)
                .getString("favorite_problems", "[]");
        Gson gson = new Gson();
        favorites = gson.fromJson(jsonString, new TypeToken<List<Problem>>(){}.getType());
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
    }

    private void saveFavorites() {
        Gson gson = new Gson();
        String jsonString = gson.toJson(favorites);
        getSharedPreferences("SparkApp2025", MODE_PRIVATE).edit()
                .putString("favorite_problems", jsonString)
                .apply();
    }

    private class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.ProblemViewHolder> {
        private List<Problem> problems = new ArrayList<>();

        public void setProblems(List<Problem> problems) {
            this.problems = problems;
            notifyDataSetChanged();
        }

        @Override
        public ProblemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorite_problem, parent, false);
            return new ProblemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ProblemViewHolder holder, int position) {
            Problem problem = problems.get(position);
            String cleanTitle = problem.getTitle().replaceAll("\\[.*?\\]", "").trim();
            holder.problemText.setText(problem.getPid() + " " + cleanTitle + " (" + problem.getDifficulty() + ")");
            holder.starButton.setTag(problem.getPid());
            holder.starButton.setTextColor(Color.YELLOW); // 初始为黄色表示已收藏
            holder.starButton.setOnClickListener(v -> {
                String pid = (String) v.getTag();
                favorites.removeIf(p -> p.getPid().equals(pid));
                saveFavorites();
                setProblems(favorites);
                holder.starButton.setTextColor(Color.WHITE); // 变为白色并移除
            });
        }

        @Override
        public int getItemCount() {
            return problems.size();
        }

        class ProblemViewHolder extends RecyclerView.ViewHolder {
            TextView problemText;
            Button starButton;

            ProblemViewHolder(View itemView) {
                super(itemView);
                problemText = itemView.findViewById(R.id.problemText);
                starButton = itemView.findViewById(R.id.starButton);
            }
        }
    }
}