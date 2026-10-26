package com.campuspulse.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 一个集中处理请求、统一响应和登录态的轻量客户端。 */
public class ApiClient {
    // 本机联调通过 adb reverse tcp:8081 tcp:8081 转发至电脑上的服务。
    public static final String BASE_URL = "http://127.0.0.1:8081";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(Object data);
        void onError(String message, int code);
    }

    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());

    public ApiClient(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("campuspulse_session", Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return !preferences.getString("token", "").isEmpty();
    }

    public String username() {
        return preferences.getString("username", "");
    }

    public void saveSession(JSONObject auth) {
        JSONObject user = auth.optJSONObject("user");
        preferences.edit()
                .putString("token", auth.optString("token"))
                .putString("username", user == null ? "" : user.optString("username"))
                .apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }

    public void get(String path, Callback callback) {
        request("GET", path, null, callback);
    }

    public void post(String path, JSONObject body, Callback callback) {
        request("POST", path, body, callback);
    }

    public void put(String path, JSONObject body, Callback callback) {
        request("PUT", path, body, callback);
    }

    public void delete(String path, Callback callback) {
        request("DELETE", path, null, callback);
    }

    private void request(String method, String path, JSONObject body, Callback callback) {
        WORKER.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(6000);
                connection.setRequestProperty("Accept", "application/json");
                String token = preferences.getString("token", "");
                if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
                int httpStatus = connection.getResponseCode();
                InputStream stream = httpStatus < 400 ? connection.getInputStream() : connection.getErrorStream();
                JSONObject envelope = new JSONObject(readAll(stream));
                int code = envelope.optInt("code", httpStatus);
                if (code == 0 && httpStatus < 400) {
                    Object data = envelope.opt("data");
                    main.post(() -> callback.onSuccess(data));
                } else {
                    if (code == 40101) clearSession();
                    String message = envelope.optString("message", "请求失败，请稍后重试");
                    main.post(() -> callback.onError(message, code));
                }
            } catch (Exception exception) {
                Log.e("CampusPulseApi", method + " " + path + " failed", exception);
                main.post(() -> callback.onError("无法连接服务，请确认后端正在运行", -1));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
