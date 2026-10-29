package com.campuspulse.server.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.campuspulse.server.domain.Activity;

/**
 * 后台活动表单。时间字段使用 HTML datetime-local 的字符串形式，
 * 避免 Thymeleaf 直接格式化 LocalDateTime 时依赖额外方言。
 */
public class AdminActivityForm {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Long id;
    private String title;
    private String description;
    private String location;
    private String type;
    private String startTime;
    private String endTime;
    private String signupDeadline;
    private Integer capacity = 50;
    private String coverUrl;
    private String organizer;
    private String status = "PUBLISHED";
    private List<Integer> tagIds = new ArrayList<>();

    public static AdminActivityForm from(Activity activity, List<Integer> tagIds) {
        AdminActivityForm form = new AdminActivityForm();
        form.id = activity.getId();
        form.title = activity.getTitle();
        form.description = activity.getDescription();
        form.location = activity.getLocation();
        form.type = activity.getType();
        form.startTime = format(activity.getStartTime());
        form.endTime = format(activity.getEndTime());
        form.signupDeadline = format(activity.getSignupDeadline());
        form.capacity = activity.getCapacity();
        form.coverUrl = activity.getCoverUrl();
        form.organizer = activity.getOrganizer();
        form.status = activity.getStatus();
        form.tagIds = new ArrayList<>(tagIds);
        return form;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : INPUT_FORMAT.format(time);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSignupDeadline() {
        return signupDeadline;
    }

    public void setSignupDeadline(String signupDeadline) {
        this.signupDeadline = signupDeadline;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Integer> tagIds) {
        this.tagIds = tagIds == null ? new ArrayList<>() : new ArrayList<>(tagIds);
    }
}
