package com.campuspulse.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.app.DatePickerDialog;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private final String[] tabs = {"精选", "活动", "日程", "我的"};
    private final List<ActivityItem> activities = new ArrayList<>();
    private final List<ActivityItem> searchResults = new ArrayList<>();
    private final List<RecommendedItem> recommendations = new ArrayList<>();
    private final List<ActivityItem> scheduleItems = new ArrayList<>();
    private final Calendar scheduleMonth = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"), Locale.CHINA);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ApiClient api;
    private LinearLayout tabRow, pageContent;
    private ScrollView pageScroll;
    private EditText searchInput;
    private JSONObject currentUser;
    private String searchedKeyword = "";
    private String listError;
    private String searchError;
    private String selectedType = "";
    private String selectedDate = "";
    private int currentTab;
    private int searchVersion;
    private boolean listLoading = true;
    private boolean recommendationsLoading;
    private String recommendationsError;
    private boolean scheduleLoading;
    private boolean scheduleLoaded;
    private String scheduleError;
    private String scheduleDate;
    private boolean showAllSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        api = new ApiClient(this);
        scheduleMonth.set(Calendar.DAY_OF_MONTH, 1);
        scheduleDate = today();
        tabRow = findViewById(R.id.tab_row);
        pageContent = findViewById(R.id.page_content);
        pageScroll = findViewById(R.id.page_scroll);
        searchInput = findViewById(R.id.search_input);
        searchInput.setCompoundDrawableTintList(ColorStateList.valueOf(color(R.color.muted)));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                searchVersion++;
                searchError = null;
                if (!query.isEmpty()) showTab(1);
                else if (currentTab == 1) renderPage();
                if (query.codePointCount(0, query.length()) >= 2) {
                    int version = searchVersion;
                    handler.postDelayed(() -> search(query, version), 350);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        showTab(0);
        loadActivities();
        if (api.isLoggedIn()) {
            LocalReminder.restoreAll(this);
            verifySession();
            loadRecommendations();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            verifySession();
            loadRecommendations();
            scheduleLoaded = false;
            showTab(3);
        } else if (requestCode == 2) {
            loadActivities();
            if (api.isLoggedIn()) loadRecommendations();
            if (currentTab == 2) loadSchedule();
        }
    }

    private void loadRecommendations() {
        if (!api.isLoggedIn()) return;
        recommendationsLoading = true;
        recommendationsError = null;
        if (currentTab == 0) renderPage();
        api.get("/api/v1/recommendations?limit=5", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                recommendationsLoading = false;
                recommendations.clear();
                if (data instanceof JSONArray) {
                    JSONArray rows = (JSONArray) data;
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.optJSONObject(i);
                        if (row == null || row.optJSONObject("activity") == null) continue;
                        recommendations.add(new RecommendedItem(row));
                    }
                }
                if (currentTab == 0) renderPage();
            }
            @Override public void onError(String message, int code) {
                recommendationsLoading = false;
                recommendationsError = message;
                if (currentTab == 0) renderPage();
            }
        });
    }

    private void loadActivities() {
        listLoading = true;
        listError = null;
        api.get("/api/v1/activities?page=1&size=50", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                listLoading = false;
                activities.clear();
                JSONObject page = data instanceof JSONObject ? (JSONObject) data : null;
                JSONArray items = page == null ? null : page.optJSONArray("items");
                if (items != null) for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null) activities.add(new ActivityItem(item));
                }
                if (currentTab == 0 || currentTab == 1) renderPage();
            }
            @Override public void onError(String message, int code) {
                listLoading = false;
                listError = message;
                if (currentTab == 0 || currentTab == 1) renderPage();
            }
        });
    }

    private void search(String query, int version) {
        if (version != searchVersion) return;
        String encoded = Uri.encode(query);
        api.get("/api/v1/activities?page=1&size=50&keyword=" + encoded, new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                if (version != searchVersion) return;
                searchedKeyword = query;
                searchError = null;
                searchResults.clear();
                JSONObject page = data instanceof JSONObject ? (JSONObject) data : null;
                JSONArray items = page == null ? null : page.optJSONArray("items");
                if (items != null) for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null) searchResults.add(new ActivityItem(item));
                }
                renderPage();
            }
            @Override public void onError(String message, int code) {
                if (version != searchVersion) return;
                searchedKeyword = query;
                searchResults.clear();
                searchError = message;
                renderPage();
            }
        });
    }

    private void verifySession() {
        api.get("/api/v1/users/me", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                currentUser = data instanceof JSONObject ? (JSONObject) data : null;
                if (currentTab == 3) renderPage();
            }
            @Override public void onError(String message, int code) {
                if (code == 40101) {
                    currentUser = null;
                    if (currentTab == 3) renderPage();
                }
            }
        });
    }

    private void showTab(int index) {
        currentTab = index;
        if (index == 2 && api.isLoggedIn() && !scheduleLoaded && !scheduleLoading) loadSchedule();
        tabRow.removeAllViews();
        for (int i = 0; i < tabs.length; i++) {
            final int tab = i;
            TextView label = text(tabs[i], 14, i == index ? R.color.ink : R.color.muted, true);
            label.setGravity(Gravity.CENTER);
            label.setBackground(i == index ? shape(R.color.khaki_light, 12, 0) : null);
            label.setOnClickListener(v -> {
                showTab(tab);
                pageScroll.scrollTo(0, 0);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1);
            if (i > 0) params.leftMargin = dp(7);
            tabRow.addView(label, params);
        }
        renderPage();
    }

    private void renderPage() {
        pageContent.removeAllViews();
        if (currentTab == 0) renderFeatured();
        else if (currentTab == 1) renderActivities();
        else if (currentTab == 2) renderSchedule();
        else renderProfile();
    }

    private void renderFeatured() {
        TextView eyebrow = text("CAMPUS  /  2026", 11, R.color.muted, true);
        eyebrow.setLetterSpacing(0.15f);
        pageContent.addView(eyebrow);
        gap(pageContent, 13);
        pageContent.addView(text("发现值得赴约的时刻", 27, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text("讲座、演出、运动与相遇，都在校园里发生。", 13, R.color.muted, false));
        gap(pageContent, 27);
        if (listLoading) { emptyCard("正在读取活动", "请稍候…", null); return; }
        if (listError != null) { emptyCard("活动暂时无法加载", listError + " · 点击重试", this::loadActivities); return; }
        if (activities.isEmpty()) { emptyCard("暂无活动", "稍后再来看看。", null); return; }

        ActivityItem featured = firstUpcoming();
        LinearLayout hero = column();
        hero.setPadding(dp(23), dp(23), dp(23), dp(23));
        hero.setBackground(shape(R.color.khaki_light, 19, 0));
        hero.addView(text("精选 · " + featured.type, 12, R.color.muted, true));
        gap(hero, 36);
        hero.addView(text(featured.title, 29, R.color.ink, true));
        gap(hero, 12);
        hero.addView(text("在 " + featured.location + "，发现下一场值得到场的校园活动。", 13, R.color.muted, false));
        gap(hero, 30);
        View line = new View(this);
        line.setBackgroundColor(color(R.color.line));
        hero.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        gap(hero, 15);
        hero.addView(text(featured.dateLabel() + "  ·  " + featured.location, 12, R.color.ink, true));
        hero.setOnClickListener(v -> openDetail(featured.id));
        pageContent.addView(hero, fullWidth());

        gap(pageContent, 31);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text("近期活动", 20, R.color.ink, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView all = text("查看全部", 12, R.color.muted, true);
        all.setOnClickListener(v -> showTab(1));
        row.addView(all);
        pageContent.addView(row, fullWidth());
        gap(pageContent, 15);
        int count = 0;
        for (ActivityItem item : activities) {
            if (item.startTime.compareTo(today()) < 0) continue;
            addActivityCard(item);
            gap(pageContent, 10);
            if (++count == 4) break;
        }
        if (count == 0) emptyCard("近期暂无活动", "全部活动中仍可浏览历史记录。", null);
        gap(pageContent, 26);
        pageContent.addView(text("为你推荐", 20, R.color.ink, true));
        gap(pageContent, 13);
        if (api.isLoggedIn()) {
            TextView refresh = text("刷新推荐  ↻", 12, R.color.muted, true);
            refresh.setOnClickListener(v -> loadRecommendations());
            pageContent.addView(refresh);
            gap(pageContent, 13);
        }
        if (!api.isLoggedIn()) {
            emptyCard("登录后查看个性化推荐", "根据你的兴趣和活动热度，为你挑选校园活动。", () -> openAuth(false));
        } else if (recommendationsLoading) {
            emptyCard("正在挑选活动", "请稍候…", null);
        } else if (recommendationsError != null) {
            emptyCard("推荐暂时无法加载", recommendationsError + " · 点击重试", this::loadRecommendations);
        } else if (recommendations.isEmpty()) {
            emptyCard("暂无新的推荐", "可以在活动页浏览更多内容，或调整兴趣方向。", () -> showTab(1));
        } else {
            for (RecommendedItem item : recommendations) {
                TextView reason = text(item.reason, 12, R.color.muted, true);
                pageContent.addView(reason);
                gap(pageContent, 8);
                addActivityCard(item.activity);
                gap(pageContent, 17);
            }
        }
    }

    private void renderActivities() {
        String query = searchInput.getText().toString().trim();
        pageContent.addView(text(query.isEmpty() ? "全部活动" : "搜索结果", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text(query.isEmpty() ? "按时间浏览校园里的每一场活动" : "搜索标题、类型、介绍和地点", 13, R.color.muted, false));
        gap(pageContent, 19);
        renderFilters();
        gap(pageContent, 20);
        if (listLoading) { emptyCard("正在读取活动", "请稍候…", null); return; }
        if (listError != null) { emptyCard("加载失败", listError + " · 点击重试", query.isEmpty() ? this::loadActivities : () -> search(query, searchVersion)); return; }
        if (searchError != null && query.length() >= 2) { emptyCard("搜索失败", searchError + " · 点击重试", () -> search(query, searchVersion)); return; }
        if (query.codePointCount(0, query.length()) >= 2 && !query.equals(searchedKeyword)) {
            emptyCard("正在搜索", "请稍候…", null);
            return;
        }
        List<ActivityItem> shown = query.codePointCount(0, query.length()) >= 2 ? searchResults : activities;
        int count = 0;
        for (ActivityItem item : shown) {
            if (query.length() == 1 && !item.matches(query)) continue;
            if (!selectedType.isEmpty() && !selectedType.equals(item.type)) continue;
            if (!selectedDate.isEmpty() && !item.startTime.startsWith(selectedDate)) continue;
            addActivityCard(item);
            gap(pageContent, 11);
            count++;
        }
        if (count == 0) emptyCard("没有找到相关活动", "试试其他关键词或地点。", null);
    }

    private void renderFilters() {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        Set<String> types = new LinkedHashSet<>();
        types.add("全部");
        for (ActivityItem item : activities) if (!item.type.isEmpty()) types.add(item.type);
        for (String type : types) {
            boolean active = type.equals("全部") ? selectedType.isEmpty() : type.equals(selectedType);
            TextView chip = text(type, 12, active ? R.color.ink : R.color.muted, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), 0, dp(16), 0);
            chip.setBackground(shape(active ? R.color.khaki_light : R.color.card, 11, 1));
            chip.setOnClickListener(v -> {
                selectedType = type.equals("全部") ? "" : type;
                renderPage();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(37));
            lp.rightMargin = dp(8);
            row.addView(chip, lp);
        }
        TextView dateChip = text(selectedDate.isEmpty() ? "选择日期" : selectedDate.substring(5) + " ×", 12, R.color.muted, true);
        dateChip.setGravity(Gravity.CENTER);
        dateChip.setPadding(dp(16), 0, dp(16), 0);
        dateChip.setBackground(shape(R.color.card, 11, 1));
        dateChip.setOnClickListener(v -> {
            if (!selectedDate.isEmpty()) { selectedDate = ""; renderPage(); return; }
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (picker, year, month, day) -> {
                selectedDate = String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, day);
                renderPage();
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });
        row.addView(dateChip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(37)));
        scroller.addView(row);
        pageContent.addView(scroller, fullWidth());
    }

    private void loadSchedule() {
        scheduleLoading = true;
        scheduleError = null;
        if (currentTab == 2) renderPage();
        api.get("/api/v1/users/me/registrations", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                scheduleLoading = false;
                scheduleLoaded = true;
                scheduleItems.clear();
                if (data instanceof JSONArray) {
                    JSONArray records = (JSONArray) data;
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.optJSONObject(i);
                        if (record == null || !"REGISTERED".equals(record.optString("status"))) continue;
                        JSONObject item = record.optJSONObject("activity");
                        if (item != null) scheduleItems.add(new ActivityItem(item));
                    }
                }
                scheduleItems.sort(Comparator.comparing(item -> item.startTime));
                if (currentTab == 2) renderPage();
            }
            @Override public void onError(String message, int code) {
                scheduleLoading = false;
                scheduleLoaded = false;
                scheduleError = message;
                if (currentTab == 2) renderPage();
            }
        });
    }

    private void renderSchedule() {
        pageContent.addView(text("活动日历", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text("查看已报名活动，选择日期了解当天安排。", 13, R.color.muted, false));
        gap(pageContent, 25);
        if (!api.isLoggedIn()) {
            emptyCard("登录后查看日历", "报名记录会显示在这里 · 点击登录", () -> openAuth(false));
            return;
        }
        renderCalendar();
        gap(pageContent, 25);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text(showAllSchedule ? "全部报名" : scheduleDate.substring(5).replace('-', '月') + "日", 18,
                R.color.ink, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView toggle = text(showAllSchedule ? "按日期查看" : "查看全部", 12, R.color.muted, true);
        toggle.setOnClickListener(v -> { showAllSchedule = !showAllSchedule; renderPage(); });
        heading.addView(toggle);
        pageContent.addView(heading, fullWidth());
        gap(pageContent, 14);
        if (scheduleLoading) { emptyCard("正在读取报名记录", "请稍候…", null); return; }
        if (scheduleError != null) {
            emptyCard("日历加载失败", scheduleError + " · 点击重试", this::loadSchedule);
            return;
        }
        int count = 0;
        for (ActivityItem item : scheduleItems) {
            if (!showAllSchedule && !item.startTime.startsWith(scheduleDate)) continue;
            addActivityCard(item);
            gap(pageContent, 11);
            count++;
        }
        if (count == 0) emptyCard(showAllSchedule ? "还没有报名活动" : "当天没有报名活动",
                showAllSchedule ? "去活动页看看下一场想参加的活动。" : "选择其他日期，或查看全部报名。",
                showAllSchedule ? () -> showTab(1) : () -> { showAllSchedule = true; renderPage(); });
    }

    private void renderCalendar() {
        LinearLayout calendar = column();
        calendar.setPadding(dp(13), dp(15), dp(13), dp(15));
        calendar.setBackground(shape(R.color.card, 16, 1));
        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = text("‹", 25, R.color.muted, true);
        previous.setGravity(Gravity.CENTER);
        previous.setOnClickListener(v -> changeScheduleMonth(-1));
        monthRow.addView(previous, new LinearLayout.LayoutParams(dp(42), dp(39)));
        TextView month = text(String.format(Locale.CHINA, "%d 年 %02d 月",
                scheduleMonth.get(Calendar.YEAR), scheduleMonth.get(Calendar.MONTH) + 1), 16, R.color.ink, true);
        month.setGravity(Gravity.CENTER);
        monthRow.addView(month, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView next = text("›", 25, R.color.muted, true);
        next.setGravity(Gravity.CENTER);
        next.setOnClickListener(v -> changeScheduleMonth(1));
        monthRow.addView(next, new LinearLayout.LayoutParams(dp(42), dp(39)));
        calendar.addView(monthRow, fullWidth());
        gap(calendar, 12);
        String[] weekdays = {"一", "二", "三", "四", "五", "六", "日"};
        LinearLayout weekdayRow = new LinearLayout(this);
        for (String name : weekdays) {
            TextView day = text(name, 11, R.color.muted, true);
            day.setGravity(Gravity.CENTER);
            weekdayRow.addView(day, new LinearLayout.LayoutParams(0, dp(25), 1));
        }
        calendar.addView(weekdayRow, fullWidth());

        int firstWeekday = (scheduleMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = scheduleMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cells = ((firstWeekday + daysInMonth + 6) / 7) * 7;
        for (int row = 0; row < cells / 7; row++) {
            LinearLayout week = new LinearLayout(this);
            for (int column = 0; column < 7; column++) {
                int dayNumber = row * 7 + column - firstWeekday + 1;
                TextView day = text("", 13, R.color.ink, false);
                day.setGravity(Gravity.CENTER);
                if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                    String date = String.format(Locale.CHINA, "%04d-%02d-%02d",
                            scheduleMonth.get(Calendar.YEAR), scheduleMonth.get(Calendar.MONTH) + 1, dayNumber);
                    boolean hasActivity = false;
                    for (ActivityItem item : scheduleItems) {
                        if (item.startTime.startsWith(date)) { hasActivity = true; break; }
                    }
                    day.setText(dayNumber + (hasActivity ? "\n●" : "\n "));
                    if (date.equals(scheduleDate) && !showAllSchedule) {
                        day.setBackground(shape(R.color.khaki_light, 10, 0));
                    }
                    day.setOnClickListener(v -> { scheduleDate = date; showAllSchedule = false; renderPage(); });
                }
                week.addView(day, new LinearLayout.LayoutParams(0, dp(48), 1));
            }
            calendar.addView(week, fullWidth());
        }
        pageContent.addView(calendar, fullWidth());
    }

    private void changeScheduleMonth(int offset) {
        scheduleMonth.add(Calendar.MONTH, offset);
        scheduleMonth.set(Calendar.DAY_OF_MONTH, 1);
        scheduleDate = String.format(Locale.CHINA, "%04d-%02d-01",
                scheduleMonth.get(Calendar.YEAR), scheduleMonth.get(Calendar.MONTH) + 1);
        showAllSchedule = false;
        renderPage();
    }

    private void renderProfile() {
        pageContent.addView(text("我的校园", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text("把喜欢的活动，留在自己的日程里。", 13, R.color.muted, false));
        gap(pageContent, 25);
        if (!api.isLoggedIn()) {
            emptyCard("尚未登录", "登录后查看报名和兴趣方向。", null);
            gap(pageContent, 18);
            button("登录", R.color.ink, R.color.card, () -> openAuth(false));
            gap(pageContent, 10);
            button("创建账号", R.color.khaki_light, R.color.ink, () -> openAuth(true));
            return;
        }
        LinearLayout card = column();
        card.setPadding(dp(22), dp(24), dp(22), dp(24));
        card.setBackground(shape(R.color.card, 16, 1));
        card.addView(text(api.username(), 22, R.color.ink, true));
        gap(card, 9);
        String email = currentUser == null || currentUser.isNull("email") ? "" : currentUser.optString("email");
        card.addView(text(email.isEmpty() ? "校园活动探索者" : email, 13, R.color.muted, false));
        pageContent.addView(card, fullWidth());
        gap(pageContent, 18);
        button("我的报名", R.color.khaki_light, R.color.ink, () -> showTab(2));
        gap(pageContent, 10);
        button("编辑联系方式", R.color.card, R.color.ink, this::showProfileEditor);
        gap(pageContent, 10);
        button("管理兴趣方向", R.color.card, R.color.ink, this::showInterestEditor);
        gap(pageContent, 10);
        button("退出登录", R.color.card, R.color.muted, this::logout);
    }

    private void showProfileEditor() {
        pageContent.removeAllViews();
        TextView back = text("←  返回我的", 13, R.color.muted, true);
        back.setOnClickListener(v -> renderPage());
        pageContent.addView(back);
        gap(pageContent, 24);
        pageContent.addView(text("编辑联系方式", 26, R.color.ink, true));
        gap(pageContent, 8);
        pageContent.addView(text("邮箱与手机号可以留空，保存后同步到账号。", 13, R.color.muted, false));
        gap(pageContent, 25);
        pageContent.addView(text("邮箱", 13, R.color.muted, true));
        gap(pageContent, 9);
        EditText email = profileField("邮箱地址", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(currentUser == null || currentUser.isNull("email") ? "" : currentUser.optString("email"));
        pageContent.addView(email, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        gap(pageContent, 20);
        pageContent.addView(text("手机号", 13, R.color.muted, true));
        gap(pageContent, 9);
        EditText phone = profileField("11 位手机号", InputType.TYPE_CLASS_PHONE);
        phone.setText(currentUser == null || currentUser.isNull("phone") ? "" : currentUser.optString("phone"));
        pageContent.addView(phone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        gap(pageContent, 26);
        button("保存资料", R.color.ink, R.color.card, () -> {
            JSONObject payload = new JSONObject();
            try {
                payload.put("email", email.getText().toString().trim());
                payload.put("phone", phone.getText().toString().trim());
            } catch (Exception ignored) { return; }
            api.put("/api/v1/users/me", payload, new ApiClient.Callback() {
                @Override public void onSuccess(Object updated) {
                    if (updated instanceof JSONObject) currentUser = (JSONObject) updated;
                    android.widget.Toast.makeText(MainActivity.this, "资料已保存", android.widget.Toast.LENGTH_SHORT).show();
                    renderPage();
                }
                @Override public void onError(String message, int code) {
                    android.widget.Toast.makeText(MainActivity.this, message, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private EditText profileField(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(inputType);
        field.setTextSize(14);
        field.setTextColor(color(R.color.ink));
        field.setHintTextColor(color(R.color.muted));
        field.setHint(hint);
        field.setPadding(dp(16), 0, dp(16), 0);
        field.setBackground(shape(R.color.card, 12, 1));
        return field;
    }

    private void showInterestEditor() {
        pageContent.removeAllViews();
        TextView back = text("←  返回我的", 13, R.color.muted, true);
        back.setOnClickListener(v -> renderPage());
        pageContent.addView(back);
        gap(pageContent, 24);
        pageContent.addView(text("兴趣方向", 26, R.color.ink, true));
        gap(pageContent, 8);
        pageContent.addView(text("选择你关注的主题，保存后会同步到账号。", 13, R.color.muted, false));
        gap(pageContent, 24);
        LinearLayout choices = column();
        pageContent.addView(choices, fullWidth());
        Set<Integer> selected = new java.util.HashSet<>();
        if (currentUser != null) {
            JSONArray existing = currentUser.optJSONArray("tagIds");
            if (existing != null) for (int i = 0; i < existing.length(); i++) selected.add(existing.optInt(i));
        }
        choices.addView(text("正在加载…", 13, R.color.muted, false));
        api.get("/api/v1/tags", new ApiClient.Callback() {
            @Override public void onSuccess(Object data) {
                if (currentTab != 3) return;
                choices.removeAllViews();
                if (!(data instanceof JSONArray)) return;
                JSONArray tags = (JSONArray) data;
                for (int i = 0; i < tags.length(); i++) {
                    JSONObject tag = tags.optJSONObject(i);
                    if (tag == null) continue;
                    int id = tag.optInt("id");
                    TextView choice = text(tag.optString("name") + (selected.contains(id) ? "  ✓" : ""), 14, R.color.ink, true);
                    choice.setPadding(dp(18), dp(14), dp(18), dp(14));
                    choice.setBackground(shape(selected.contains(id) ? R.color.khaki_light : R.color.card, 12, 1));
                    choice.setOnClickListener(v -> {
                        if (!selected.add(id)) selected.remove(id);
                        choice.setText(tag.optString("name") + (selected.contains(id) ? "  ✓" : ""));
                        choice.setBackground(shape(selected.contains(id) ? R.color.khaki_light : R.color.card, 12, 1));
                    });
                    choices.addView(choice, fullWidth());
                    gap(choices, 9);
                }
                gap(pageContent, 17);
                button("保存兴趣", R.color.ink, R.color.card, () -> {
                    JSONObject payload = new JSONObject();
                    JSONArray ids = new JSONArray();
                    for (Integer id : selected) ids.put(id);
                    try { payload.put("tagIds", ids); } catch (Exception ignored) { return; }
                    api.put("/api/v1/users/me/interests", payload, new ApiClient.Callback() {
                        @Override public void onSuccess(Object updated) {
                            if (updated instanceof JSONObject) currentUser = (JSONObject) updated;
                            loadRecommendations();
                            renderPage();
                        }
                        @Override public void onError(String message, int code) {
                            android.widget.Toast.makeText(MainActivity.this, message, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
            @Override public void onError(String message, int code) {
                if (currentTab != 3) return;
                choices.removeAllViews();
                choices.addView(text(message, 13, R.color.error, false));
            }
        });
    }

    private void logout() {
        api.post("/api/v1/auth/logout", null, new ApiClient.Callback() {
            @Override public void onSuccess(Object data) { finishLogout(); }
            @Override public void onError(String message, int code) { finishLogout(); }
        });
    }

    private void finishLogout() {
        LocalReminder.cancelAll(this);
        api.clearSession();
        currentUser = null;
        scheduleItems.clear();
        scheduleLoaded = false;
        recommendations.clear();
        recommendationsError = null;
        recommendationsLoading = false;
        showTab(3);
    }

    private void openAuth(boolean register) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("register", register);
        startActivityForResult(intent, 1);
    }

    private void openDetail(long id) {
        Intent intent = new Intent(this, ActivityDetailActivity.class);
        intent.putExtra("activityId", id);
        startActivityForResult(intent, 2);
    }

    private ActivityItem firstUpcoming() {
        String now = serverTime("yyyy-MM-dd HH:mm:ss");
        for (ActivityItem item : activities) {
            if (item.startTime.compareTo(now) >= 0 && item.signupDeadline.compareTo(now) >= 0) return item;
        }
        for (ActivityItem item : activities) if (item.startTime.compareTo(now) >= 0) return item;
        return activities.get(0);
    }

    private String today() {
        return serverTime("yyyy-MM-dd");
    }

    private String serverTime(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.CHINA);
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return formatter.format(new Date());
    }

    private void addActivityCard(ActivityItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(15), dp(16), dp(15), dp(16));
        card.setBackground(shape(R.color.card, 16, 1));
        card.setOnClickListener(v -> openDetail(item.id));
        LinearLayout date = column();
        date.setGravity(Gravity.CENTER);
        date.setBackground(shape(R.color.khaki_light, 11, 0));
        date.addView(text(item.month() + "月", 11, R.color.muted, true));
        date.addView(text(item.day(), 21, R.color.ink, true));
        card.addView(date, new LinearLayout.LayoutParams(dp(55), dp(62)));

        LinearLayout details = column();
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        detailsParams.leftMargin = dp(15);
        card.addView(details, detailsParams);
        details.addView(text(item.type.isEmpty() ? item.time() : item.type + "  ·  " + item.time(), 11, R.color.muted, false));
        gap(details, 7);
        details.addView(text(item.title, 16, R.color.ink, true));
        gap(details, 10);
        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_location_vector);
        icon.setImageTintList(ColorStateList.valueOf(color(R.color.muted)));
        locationRow.addView(icon, new LinearLayout.LayoutParams(dp(14), dp(14)));
        TextView location = text(item.location, 11, R.color.muted, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        locationRow.addView(location, lp);
        details.addView(locationRow);
        if (item.remaining == 0 && item.capacity > 0) {
            gap(details, 6);
            details.addView(text("名额已满", 11, R.color.muted, true));
        }
        pageContent.addView(card, fullWidth());
    }

    private void emptyCard(String title, String subtitle, Runnable click) {
        LinearLayout card = column();
        card.setPadding(dp(22), dp(28), dp(22), dp(28));
        card.setBackground(shape(R.color.card, 16, 1));
        card.addView(text(title, 18, R.color.ink, true));
        gap(card, 9);
        card.addView(text(subtitle, 13, R.color.muted, false));
        if (click != null) card.setOnClickListener(v -> click.run());
        pageContent.addView(card, fullWidth());
    }

    private void button(String label, int fill, int foreground, Runnable click) {
        TextView button = text(label, 14, foreground, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(shape(fill, 13, fill == R.color.card ? 1 : 0));
        button.setOnClickListener(v -> click.run());
        pageContent.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)));
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

    private static class ActivityItem {
        final long id;
        final String title, type, location, startTime, signupDeadline;
        final int capacity, remaining;

        ActivityItem(JSONObject json) {
            id = json.optLong("id");
            title = json.optString("title");
            type = json.optString("type");
            location = json.optString("location");
            startTime = json.optString("startTime");
            signupDeadline = json.optString("signupDeadline");
            capacity = json.optInt("capacity");
            remaining = json.has("remaining") ? json.optInt("remaining") : -1;
        }

        String month() { return startTime.length() >= 7 ? startTime.substring(5, 7) : "--"; }
        String day() { return startTime.length() >= 10 ? startTime.substring(8, 10) : "--"; }
        String time() { return startTime.length() >= 16 ? startTime.substring(11, 16) : "待定"; }
        String dateLabel() { return month() + "月" + day() + "日"; }
        boolean matches(String query) { return title.contains(query) || type.contains(query) || location.contains(query); }
    }

    private static class RecommendedItem {
        final ActivityItem activity;
        final String reason;

        RecommendedItem(JSONObject json) {
            activity = new ActivityItem(json.optJSONObject("activity"));
            reason = json.optString("reason", "为你推荐");
        }
    }
}
