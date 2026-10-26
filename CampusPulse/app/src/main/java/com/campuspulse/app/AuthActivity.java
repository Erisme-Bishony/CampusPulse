package com.campuspulse.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AuthActivity extends AppCompatActivity {
    private final Set<Integer> selectedTags = new HashSet<>();
    private ApiClient api;
    private LinearLayout content;
    private LinearLayout tagArea;
    private EditText usernameInput, passwordInput, confirmInput, emailInput, phoneInput;
    private TextView errorView, submitButton;
    private boolean registerMode;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        api = new ApiClient(this);
        registerMode = getIntent().getBooleanExtra("register", false);

        ScrollView scroll = new ScrollView(this);
        scroll.setId(View.generateViewId());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.surface));
        content = column();
        content.setPadding(dp(24), dp(20), dp(24), dp(36));
        scroll.addView(content);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        showForm();
    }

    private void showForm() {
        content.removeAllViews();
        TextView back = text("←  返回首页", 13, R.color.muted, true);
        back.setOnClickListener(v -> finish());
        content.addView(back);
        gap(content, 44);

        TextView brand = text("CampusPulse", 16, R.color.ink, true);
        content.addView(brand);
        gap(content, 16);
        content.addView(text(registerMode ? "加入校园的每一刻" : "欢迎回来", 30, R.color.ink, true));
        gap(content, 9);
        content.addView(text(registerMode ? "创建账号，发现与你同频的活动。" : "登录后，继续你的校园日程。", 13, R.color.muted, false));
        gap(content, 30);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setBackground(shape(R.color.khaki_light, 13, 0));
        modeRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        addMode(modeRow, "登录", false);
        addMode(modeRow, "注册", true);
        content.addView(modeRow, fullWidth());
        gap(content, 25);

        usernameInput = field("用户名", "请输入用户名", false);
        passwordInput = field("密码", registerMode ? "6–64 个字符" : "请输入密码", true);
        if (registerMode) {
            confirmInput = field("确认密码", "再次输入密码", true);
            emailInput = field("邮箱 · 选填", "name@example.com", false);
            emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            phoneInput = field("手机号 · 选填", "11 位手机号", false);
            phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
            content.addView(text("兴趣方向 · 选填", 13, R.color.ink, true));
            gap(content, 10);
            tagArea = column();
            content.addView(tagArea, fullWidth());
            loadTags();
            gap(content, 19);
        }

        errorView = text("", 12, R.color.error, false);
        errorView.setVisibility(View.GONE);
        content.addView(errorView);
        submitButton = text(registerMode ? "创建账号" : "登录", 15, R.color.card, true);
        submitButton.setGravity(Gravity.CENTER);
        submitButton.setBackground(shape(R.color.ink, 13, 0));
        submitButton.setOnClickListener(v -> submit());
        content.addView(submitButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        gap(content, 20);
        TextView hint = text(registerMode ? "已有账号？返回登录" : "还没有账号？立即注册", 13, R.color.muted, false);
        hint.setGravity(Gravity.CENTER);
        hint.setOnClickListener(v -> switchMode());
        content.addView(hint);
    }

    private void addMode(LinearLayout row, String label, boolean isRegister) {
        TextView button = text(label, 14, registerMode == isRegister ? R.color.ink : R.color.muted, true);
        button.setGravity(Gravity.CENTER);
        if (registerMode == isRegister) button.setBackground(shape(R.color.card, 10, 0));
        button.setOnClickListener(v -> {
            if (registerMode != isRegister) switchMode();
        });
        row.addView(button, new LinearLayout.LayoutParams(0, dp(40), 1));
    }

    private void switchMode() {
        if (busy) return;
        registerMode = !registerMode;
        showForm();
    }

    private EditText field(String label, String hint, boolean password) {
        content.addView(text(label, 13, R.color.ink, true));
        gap(content, 9);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(color(R.color.ink));
        input.setHintTextColor(color(R.color.muted));
        input.setHint(hint);
        input.setPadding(dp(17), 0, dp(17), 0);
        input.setBackground(shape(R.color.card, 12, 1));
        if (password) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(51)));
        gap(content, 20);
        return input;
    }

    private void loadTags() {
        tagArea.addView(text("正在加载兴趣方向…", 12, R.color.muted, false));
        api.get("/api/v1/tags", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                if (!registerMode || tagArea == null) return;
                tagArea.removeAllViews();
                if (!(data instanceof JSONArray)) return;
                JSONArray tags = (JSONArray) data;
                LinearLayout row = null;
                for (int i = 0; i < tags.length(); i++) {
                    JSONObject tag = tags.optJSONObject(i);
                    if (tag == null) continue;
                    if (i % 2 == 0) {
                        row = new LinearLayout(AuthActivity.this);
                        tagArea.addView(row, fullWidth());
                        gap(tagArea, 8);
                    }
                    int id = tag.optInt("id");
                    TextView chip = text(tag.optString("name"), 12, R.color.muted, true);
                    chip.setGravity(Gravity.CENTER);
                    chip.setBackground(shape(selectedTags.contains(id) ? R.color.khaki : R.color.card, 10, 1));
                    chip.setOnClickListener(v -> {
                        if (!selectedTags.add(id)) selectedTags.remove(id);
                        chip.setBackground(shape(selectedTags.contains(id) ? R.color.khaki : R.color.card, 10, 1));
                        chip.setTextColor(color(selectedTags.contains(id) ? R.color.ink : R.color.muted));
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(39), 1);
                    if (i % 2 == 1) params.leftMargin = dp(8);
                    if (row != null) row.addView(chip, params);
                }
            }
            @Override public void onError(String message, int code) {
                if (registerMode && tagArea != null) {
                    tagArea.removeAllViews();
                    TextView retry = text("兴趣方向加载失败 · 点击重试", 12, R.color.error, false);
                    retry.setOnClickListener(v -> loadTags());
                    tagArea.addView(retry);
                }
            }
        });
    }

    private void submit() {
        if (busy) return;
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (username.isEmpty() || password.isEmpty()) { showError("请输入用户名和密码"); return; }
        if (registerMode && (username.length() < 3 || username.length() > 50)) { showError("用户名需为 3–50 个字符"); return; }
        if (registerMode && (password.length() < 6 || password.length() > 64)) { showError("密码需为 6–64 个字符"); return; }
        if (registerMode && !password.equals(confirmInput.getText().toString())) { showError("两次输入的密码不一致"); return; }

        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
            if (registerMode) {
                String email = emailInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError("邮箱格式不正确"); return; }
                if (!phone.isEmpty() && !phone.matches("1\\d{10}")) { showError("请输入 11 位手机号"); return; }
                body.put("confirmPassword", confirmInput.getText().toString());
                body.put("email", email);
                body.put("phone", phone);
                JSONArray tagIds = new JSONArray();
                for (Integer id : selectedTags) tagIds.put(id);
                body.put("tagIds", tagIds);
            }
        } catch (Exception exception) { showError("表单处理失败"); return; }

        busy = true;
        errorView.setVisibility(View.GONE);
        submitButton.setText(registerMode ? "正在创建…" : "正在登录…");
        api.post(registerMode ? "/api/v1/auth/register" : "/api/v1/auth/login", body, new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                busy = false;
                if (!(data instanceof JSONObject) || ((JSONObject) data).optString("token").isEmpty()) {
                    showError("服务端没有返回登录凭据");
                    return;
                }
                api.saveSession((JSONObject) data);
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(String message, int code) {
                busy = false;
                submitButton.setText(registerMode ? "创建账号" : "登录");
                showError(message);
            }
        });
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
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

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int color(int id) { return ContextCompat.getColor(this, id); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
