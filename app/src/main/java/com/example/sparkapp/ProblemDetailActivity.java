package com.example.sparkapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProblemDetailActivity extends AppCompatActivity {
    private static final String LUOGU_API_URL = "https://www.luogu.com.cn/problem/";
    private static final OkHttpClient client = new OkHttpClient();
    private static final String PPIO_API_URL = "https://api.ppinfra.com/v3/openai/chat/completions";
    private static final String PPIO_API_KEY = "sk_6WVjXH0nJLf8fP2q9qgYzXjRs_2ASpVd_s47ZS9OFHw"; // PPIO API 密钥

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("题目详情");
        }

        TextView titleText = findViewById(R.id.problemTitle);
        TextView descriptionText = findViewById(R.id.problemDescription);
        TextView problemUrlText = findViewById(R.id.problemUrl);
        TextView solutionUrlText = findViewById(R.id.solutionUrl);

        String pid = getIntent().getStringExtra("pid");
        String title = getIntent().getStringExtra("title");

        titleText.setText(title);

        // 设置默认描述
        descriptionText.setText("AI 概述：加载中...");

        // 构造题目详情 API 请求
        String apiUrl = LUOGU_API_URL + pid + "?_contentOnly=1";
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("referer", "https://www.luogu.com.cn/")
                .addHeader("x-luogu-type", "content-only")
                .addHeader("user-agent", "SparkApp/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("ProblemDetail", "API Call Failed: " + e.getMessage());
                    descriptionText.setText("AI 概述：加载失败: " + e.getMessage());
                    problemUrlText.setText("题目网址: https://www.luogu.com.cn/problem/" + pid);
                    solutionUrlText.setText("答案网址: https://www.luogu.com.cn/discuss?pid=" + pid);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (Response responseBody = response) {
                        String htmlString = responseBody.body().string();
                        Log.d("ProblemDetail", "API Response: " + htmlString.substring(0, 1000));
                        // 使用 Jsoup 解析 HTML
                        Document doc = Jsoup.parse(htmlString);
                        Element descriptionElement = doc.selectFirst("section:contains(题目描述) > div");
                        String originalDescription = descriptionElement != null ? descriptionElement.text().trim() : "暂无描述";

                        // 立即设置网址
                        runOnUiThread(() -> {
                            problemUrlText.setText("题目网址: https://www.luogu.com.cn/problem/" + pid);
                            solutionUrlText.setText("答案网址: https://www.luogu.com.cn/discuss?pid=" + pid);
                        });

                        // 调用 PPIO AI 获取概述
                        getPpioOverview(originalDescription, overview -> {
                            runOnUiThread(() -> {
                                if (overview != null && !overview.isEmpty()) {
                                    descriptionText.setText("AI 概述: " + overview);
                                } else {
                                    Log.w("ProblemDetail", "Overview is empty or null");
                                    descriptionText.setText("AI 概述: 无法生成概述，请检查网络或 API 密钥");
                                }
                            });
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Log.e("ProblemDetail", "Parse Error: " + e.getMessage());
                            descriptionText.setText("AI 概述：解析失败: " + e.getMessage());
                            problemUrlText.setText("题目网址: https://www.luogu.com.cn/problem/" + pid);
                            solutionUrlText.setText("答案网址: https://www.luogu.com.cn/discuss?pid" + pid);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Log.e("ProblemDetail", "API Error: " + response.code() + ", URL: " + apiUrl);
                        descriptionText.setText("AI 概述：API 错误: " + response.code());
                        problemUrlText.setText("题目网址: https://www.luogu.com.cn/problem/" + pid);
                        solutionUrlText.setText("答案网址: https://www.luogu.com.cn/discuss?pid" + pid);
                    });
                }
            }
        });
    }

    // 调用 PPIO AI 获取概述
    private void getPpioOverview(String description, CallbackWithResult<String> callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String prompt = "请简要概述以下编程题目描述（100字以内），并说明大概需要什么算法知识，让用户大致了解题目内容：" + description;

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

        Request request = new Request.Builder()
                .url(PPIO_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + PPIO_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ProblemDetail", "Qwen API Call Failed: " + e.getMessage());
                callback.onResult("无法生成概述，请稍后重试");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    StringBuilder fullResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String jsonStr = line.substring(6);
                            if (!jsonStr.equals("[DONE]")) {
                                try {
                                    JSONObject json = new JSONObject(jsonStr);
                                    JSONArray choices = json.getJSONArray("choices");
                                    if (choices != null && !choices.isEmpty()) {
                                        JSONObject choice = choices.getJSONObject(0);
                                        JSONObject delta = choice.getJSONObject("delta");
                                        if (delta != null) {
                                            String content = delta.getStr("content", "");
                                            fullResponse.append(content);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("ProblemDetail", "Parse Qwen Response Error: " + e.getMessage());
                                }
                            }
                        }
                    }
                    reader.close();
                    String overview = fullResponse.toString().trim();
                    callback.onResult(overview.isEmpty() ? "概述生成失败" : overview);
                } else {
                    Log.e("ProblemDetail", "Qwen API Error: " + response.code());
                    callback.onResult("API 错误: " + response.code());
                }
            }
        });
    }

    // 回调接口，用于处理异步结果
    private interface CallbackWithResult<T> {
        void onResult(T result);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}