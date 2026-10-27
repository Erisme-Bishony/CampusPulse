package com.campuspulse.server.admin;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.repository.UserTagRepository;

@Service
public class AdminTagService {

    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final TagRepository tagRepository;
    private final ActivityTagRepository activityTagRepository;
    private final UserTagRepository userTagRepository;

    public AdminTagService(TagRepository tagRepository,
                           ActivityTagRepository activityTagRepository,
                           UserTagRepository userTagRepository) {
        this.tagRepository = tagRepository;
        this.activityTagRepository = activityTagRepository;
        this.userTagRepository = userTagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> list() {
        return tagRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public Tag create(String name, String color, Integer sortOrder) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (normalizedName.codePointCount(0, normalizedName.length()) > 30) {
            throw new IllegalArgumentException("标签名称不能超过 30 个字");
        }
        if (tagRepository.existsByName(normalizedName)) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        String normalizedColor = color == null || color.isBlank() ? "#4A90D9" : color.trim();
        if (!COLOR.matcher(normalizedColor).matches()) {
            throw new IllegalArgumentException("颜色必须是 #RRGGBB 格式");
        }
        int order = sortOrder == null ? 0 : sortOrder;
        if (order < 0) {
            throw new IllegalArgumentException("排序值不能为负数");
        }

        Tag tag = new Tag();
        tag.setName(normalizedName);
        tag.setColor(normalizedColor);
        tag.setSortOrder(order);
        return tagRepository.save(tag);
    }

    @Transactional
    public void delete(Integer tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
        if (activityTagRepository.existsByIdTagId(tagId) || userTagRepository.existsByTagId(tagId)) {
            throw new IllegalArgumentException("标签 \"" + tag.getName() + "\" 已被活动或用户使用，不能删除");
        }
        tagRepository.delete(tag);
    }
}
