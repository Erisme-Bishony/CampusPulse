package com.campuspulse.app;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // 2026-10-22 从本地 campuspulse 数据库读取的 12 条 PUBLISHED 活动快照。
    // 当前首页只做静态展示；接入后端时，用接口返回值替换这里即可。
    private static final ActivityItem[] ACTIVITIES = {
            new ActivityItem("10", "24", "社团招新嘉年华", "招新", "中心广场", "13:52", 1, 500),
            new ActivityItem("10", "25", "秋季篮球联赛小组赛", "比赛", "东区篮球场", "13:52", 5, 5),
            new ActivityItem("10", "27", "人工智能前沿讲座", "讲座", "图书馆报告厅", "13:52", 3, 120),
            new ActivityItem("10", "28", "图书馆志愿整理活动", "志愿", "图书馆三层", "13:52", 2, 20),
            new ActivityItem("10", "29", "校园歌手大赛决赛", "演出", "大学生活动中心剧场", "13:52", 2, 300),
            new ActivityItem("10", "30", "心理健康主题沙龙", "沙龙", "学生服务中心 301", "13:52", 0, 30),
            new ActivityItem("10", "31", "旧物交换市集", "市集", "中心广场", "13:52", 2, 100),
            new ActivityItem("11", "01", "创业计划书工作坊", "工作坊", "创新创业学院 204", "13:52", 1, 40),
            new ActivityItem("11", "03", "互联网企业校园宣讲会", "宣讲", "就业指导中心报告厅", "13:52", 3, 150),
            new ActivityItem("11", "05", "机器学习入门分享会", "讲座", "教学楼 B203", "13:52", 2, 80),
            new ActivityItem("11", "07", "话剧社年度公演", "演出", "大学生活动中心剧场", "13:52", 0, 200),
            new ActivityItem("11", "11", "校园迷你马拉松", "比赛", "田径场东门", "13:52", 1, 300)
    };

    private final String[] tabNames = {"精选", "活动", "日程", "我的"};
    private LinearLayout tabRow;
    private LinearLayout pageContent;
    private ScrollView pageScroll;
    private EditText searchInput;
    private int currentTab = 0;

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

        tabRow = findViewById(R.id.tab_row);
        pageContent = findViewById(R.id.page_content);
        pageScroll = findViewById(R.id.page_scroll);
        searchInput = findViewById(R.id.search_input);
        searchInput.setCompoundDrawableTintList(ColorStateList.valueOf(color(R.color.muted)));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 || currentTab == 1) showTab(1);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        showTab(0);
    }

    private void showTab(int index) {
        currentTab = index;
        tabRow.removeAllViews();
        for (int i = 0; i < tabNames.length; i++) {
            final int tab = i;
            TextView label = text(tabNames[i], 14, i == index ? R.color.ink : R.color.muted, true);
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

        pageContent.removeAllViews();
        if (index == 0) renderFeatured();
        else if (index == 1) renderActivities();
        else if (index == 2) renderSchedule();
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

        LinearLayout feature = column();
        feature.setPadding(dp(23), dp(23), dp(23), dp(23));
        feature.setBackground(shape(R.color.khaki_light, 19, 0));
        feature.addView(text("本周 · 社团与兴趣", 12, R.color.muted, true));
        gap(feature, 36);
        feature.addView(text("社团招新\n嘉年华", 29, R.color.ink, true));
        gap(feature, 12);
        feature.addView(text("四十余个学生社团集中摆摊，涵盖学术、艺术、公益与体育。", 13, R.color.muted, false));
        gap(feature, 30);
        View line = new View(this);
        line.setBackgroundColor(color(R.color.line));
        feature.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        gap(feature, 15);
        feature.addView(text("10月24日  ·  中心广场", 12, R.color.ink, true));
        pageContent.addView(feature, fullWidth());

        gap(pageContent, 31);
        sectionTitle("近期活动", "查看全部", () -> showTab(1));
        gap(pageContent, 15);
        for (int i = 0; i < 4; i++) {
            addActivityCard(ACTIVITIES[i]);
            gap(pageContent, 10);
        }
    }

    private void renderActivities() {
        String query = searchInput.getText().toString().trim();
        int matches = 0;
        for (ActivityItem item : ACTIVITIES) if (item.matches(query)) matches++;
        pageContent.addView(text(query.isEmpty() ? "全部活动" : "搜索结果", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text(query.isEmpty() ? "按时间浏览校园里的每一场活动" : "找到 " + matches + " 场相关活动", 13, R.color.muted, false));
        gap(pageContent, 25);
        if (matches == 0) {
            emptyCard("没有找到相关活动", "试试其他关键词或地点。");
            return;
        }
        for (ActivityItem item : ACTIVITIES) {
            if (!item.matches(query)) continue;
            addActivityCard(item);
            gap(pageContent, 11);
        }
    }

    private void renderSchedule() {
        pageContent.addView(text("近期日程", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text("公开活动时间线 · 个人报名日程将在后续接入", 13, R.color.muted, false));
        gap(pageContent, 25);
        for (int i = 0; i < 5; i++) {
            addActivityCard(ACTIVITIES[i]);
            gap(pageContent, 11);
        }
    }

    private void renderProfile() {
        pageContent.addView(text("我的校园", 26, R.color.ink, true));
        gap(pageContent, 7);
        pageContent.addView(text("把喜欢的活动，留在自己的日程里。", 13, R.color.muted, false));
        gap(pageContent, 25);
        emptyCard("个人中心即将接入", "登录、收藏与报名记录将在后续模块完成。");
    }

    private void sectionTitle(String title, String action, Runnable onAction) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 20, R.color.ink, true);
        row.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView link = text(action, 12, R.color.muted, true);
        link.setOnClickListener(v -> onAction.run());
        row.addView(link);
        pageContent.addView(row, fullWidth());
    }

    private void addActivityCard(ActivityItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(15), dp(16), dp(15), dp(16));
        card.setBackground(shape(R.color.card, 16, 1));

        LinearLayout date = column();
        date.setGravity(Gravity.CENTER);
        date.setBackground(shape(R.color.khaki_light, 11, 0));
        date.addView(text(item.month + "月", 11, R.color.muted, true));
        date.addView(text(item.day, 21, R.color.ink, true));
        card.addView(date, new LinearLayout.LayoutParams(dp(55), dp(62)));

        LinearLayout details = column();
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        detailsParams.leftMargin = dp(15);
        card.addView(details, detailsParams);
        details.addView(text(item.category + "  ·  " + item.time, 11, R.color.muted, false));
        gap(details, 7);
        details.addView(text(item.title, 16, R.color.ink, true));
        gap(details, 10);
        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView locationIcon = new ImageView(this);
        locationIcon.setImageResource(R.drawable.ic_location_vector);
        locationIcon.setImageTintList(ColorStateList.valueOf(color(R.color.muted)));
        locationRow.addView(locationIcon, new LinearLayout.LayoutParams(dp(14), dp(14)));
        TextView location = text(item.location, 11, R.color.muted, false);
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        locationParams.leftMargin = dp(4);
        locationRow.addView(location, locationParams);
        details.addView(locationRow);
        if (item.registered >= item.capacity) {
            gap(details, 6);
            details.addView(text("名额已满", 11, R.color.muted, true));
        }
        pageContent.addView(card, fullWidth());
    }

    private void emptyCard(String title, String subtitle) {
        LinearLayout card = column();
        card.setPadding(dp(22), dp(30), dp(22), dp(30));
        card.setBackground(shape(R.color.card, 16, 1));
        card.addView(text(title, 18, R.color.ink, true));
        gap(card, 9);
        card.addView(text(subtitle, 13, R.color.muted, false));
        pageContent.addView(card, fullWidth());
    }

    private TextView text(String value, int sizeSp, int colorId, boolean medium) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color(colorId));
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable shape(int fillColor, int radiusDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(fillColor));
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), color(R.color.line));
        return drawable;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void gap(LinearLayout parent, int heightDp) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int color(int id) { return ContextCompat.getColor(this, id); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static class ActivityItem {
        final String month, day, title, category, location, time;
        final int registered, capacity;

        ActivityItem(String month, String day, String title, String category, String location,
                     String time, int registered, int capacity) {
            this.month = month;
            this.day = day;
            this.title = title;
            this.category = category;
            this.location = location;
            this.time = time;
            this.registered = registered;
            this.capacity = capacity;
        }

        boolean matches(String query) {
            return query.isEmpty() || title.contains(query) || category.contains(query) || location.contains(query);
        }
    }
}
