package com.campuspulse.server.tag;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.tag.dto.TagResponse;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listAll() {
        return tagRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName(), tag.getColor()))
                .toList();
    }
}
