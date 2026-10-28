package com.campuspulse.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ActivityDetailActivity extends AppCompatActivity {
    private ApiClient api;
    private LinearLayout content;
    private JSONObject activity;
    private long activityId;
    private boolean registered;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        activityId = getIntent().getLongExtra("activityId", 0);
        if (activityId == 0) { finish(); return; }
        api = new ApiClient(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.surface));
        scroll.setFillViewport(true);
        content = column();
        content.setPadding(dp(24), dp(20), dp(24), dp(38));
        scroll.addView(content);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        loadDetail();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) loadStatus();
    }

    private void loadDetail() {
        content.removeAllViews();
        content.addView(text("正在加载活动…", 14, R.color.muted, false));
        api.get("/api/v1/activities/" + activityId, new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                if (!(data instanceof JSONObject)) { showError("活动数据格式不正确"); return; }
                activity = (JSONObject) data;
                render();
                if (api.isLoggedIn()) loadStatus();
            }
            @Override public void onError(String message, int code) { showError(message); }
        });
    }

    private void loadStatus() {
        api.get("/api/v1/activities/" + activityId + "/registration-status", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                registered = data instanceof JSONObject && ((JSONObject) data).optBoolean("registered");
                render();
            }
            @Override public void onError(String message, int code) {
                if (code == 40101) registered = false;
                render();
            }
        });
    }

    private void render() {
        if (activity == null) return;
        content.removeAllViews();
        TextView back = text("←  返回活动", 13, R.color.muted, true);
        back.setOnClickListener(v -> finish());
        content.addView(back);
        gap(36);
        content.addView(text(activity.optString("type") + "  /  CAMPUSPULSE", 11, R.color.muted, true));
        gap(15);
        content.addView(text(activity.optString("title"), 29, R.color.ink, true));
        gap(24);

        LinearLayout info = column();
        info.setPadding(dp(21), dp(22), dp(21), dp(22));
        info.setBackground(shape(R.color.khaki_light, 17, 0));
        addFact(info, "时间", formatTime(activity.optString("startTime")));
        gap(info, 17);
        addFact(info, "地点", activity.optString("location", "待定"));
        gap(info, 17);
        addFact(info, "主办", activity.optString("organizer", "待定"));
        gap(info, 17);
        int remaining = activity.optInt("remaining");
        addFact(info, "名额", remaining > 0 ? "剩余 " + remaining + " 个" : "名额已满");
        content.addView(info, fullWidth());
        gap(30);

        content.addView(text("活动介绍", 20, R.color.ink, true));
        gap(13);
        String description = activity.optString("description", "暂无活动介绍");
        TextView body = text(description, 14, R.color.muted, false);
        body.setLineSpacing(dp(6), 1f);
        content.addView(body);
        JSONArray tags = activity.optJSONArray("tags");
        if (tags != null && tags.length() > 0) {
            gap(27);
            content.addView(text("相关主题", 20, R.color.ink, true));
            gap(12);
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < tags.length(); i++) {
                JSONObject tag = tags.optJSONObject(i);
                if (tag == null) continue;
                if (names.length() > 0) names.append("  ·  ");
                names.append(tag.optString("name"));
            }
            content.addView(text(names.toString(), 13, R.color.muted, false));
        }
        gap(35);
        TextView action = text(actionLabel(), 15, R.color.card, true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(shape(R.color.ink, 13, 0));
        action.setOnClickListener(v -> onAction());
        content.addView(action, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        gap(12);
        TextView share = text("分享活动", 13, R.color.muted, true);
        share.setGravity(Gravity.CENTER);
        share.setOnClickListener(v -> share());
        content.addView(share);
    }

    private String actionLabel() {
        if (busy) return "正在处理…";
        if (!api.isLoggedIn()) return "登录后报名";
        if (registered) return "取消报名";
        if (activity.optInt("remaining") <= 0) return "名额已满";
        String deadline = activity.optString("signupDeadline");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String now = formatter.format(new Date());
        if (!deadline.isEmpty() && deadline.compareTo(now) < 0) return "报名已截止";
        return "报名参加";
    }

    private void onAction() {
        if (busy) return;
        if (!api.isLoggedIn()) {
            startActivityForResult(new Intent(this, AuthActivity.class), 1);
            return;
        }
        if (registered) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("确定取消这场活动的报名吗？")
                    .setNegativeButton("保留报名", null)
                    .setPositiveButton("取消报名", (clickedDialog, which) -> changeRegistration(false))
                    .create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color(R.color.muted));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color(R.color.ink));
            return;
        }
        if (activity.optInt("remaining") > 0 && "报名参加".equals(actionLabel())) changeRegistration(true);
    }

    private void changeRegistration(boolean signUp) {
        busy = true;
        render();
        String path = "/api/v1/activities/" + activityId + "/registrations";
        ApiClient.Callback callback = new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                busy = false;
                registered = signUp;
                Toast.makeText(ActivityDetailActivity.this, signUp ? "报名成功" : "已取消报名", Toast.LENGTH_SHORT).show();
                loadDetail();
            }
            @Override public void onError(String message, int code) {
                busy = false;
                Toast.makeText(ActivityDetailActivity.this, message, Toast.LENGTH_LONG).show();
                render();
            }
        };
        if (signUp) api.post(path, null, callback);
        else api.delete(path, callback);
    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, activity.optString("title") + "\n" + formatTime(activity.optString("startTime"))
                + " · " + activity.optString("location") + "\n来自 CampusPulse");
        startActivity(Intent.createChooser(intent, "分享活动"));
    }

    private String formatTime(String value) {
        if (value.length() < 16) return value;
        return value.substring(0, 10).replace('-', '/') + "  " + value.substring(11, 16);
    }

    private void addFact(LinearLayout parent, String label, String value) {
        parent.addView(text(label, 11, R.color.muted, true));
        gap(parent, 5);
        parent.addView(text(value, 15, R.color.ink, true));
    }

    private void showError(String message) {
        content.removeAllViews();
        TextView retry = text(message + " · 点击重试", 14, R.color.error, false);
        retry.setOnClickListener(v -> loadDetail());
        content.addView(retry);
    }

    private TextView text(String value, int size, int colorId, boolean medium) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(colorId));
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable shape(int fill, int radius, int stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(fill));
        background.setCornerRadius(dp(radius));
        if (stroke > 0) background.setStroke(dp(stroke), color(R.color.line));
        return background;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void gap(int height) { gap(content, height); }
    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }
    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private int color(int id) { return ContextCompat.getColor(this, id); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
