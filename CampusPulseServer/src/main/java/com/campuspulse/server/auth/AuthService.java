package com.campuspulse.server.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.auth.dto.AuthResponse;
import com.campuspulse.server.auth.dto.LoginRequest;
import com.campuspulse.server.auth.dto.RegisterRequest;
import com.campuspulse.server.auth.dto.UpdateInterestsRequest;
import com.campuspulse.server.auth.dto.UpdateProfileRequest;
import com.campuspulse.server.auth.dto.UserResponse;
import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.common.JwtUtil;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.domain.User;
import com.campuspulse.server.domain.UserTag;
import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.repository.UserRepository;
import com.campuspulse.server.repository.UserTagRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserTagRepository userTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       TagRepository tagRepository,
                       UserTagRepository userTagRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.userTagRepository = userTagRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (!request.password().equals(request.confirmPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        List<Integer> tagIds = normalizeTagIds(request.tagIds());
        validateTags(tagIds);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setStatus((byte) 1);
        userRepository.save(user);

        userTagRepository.saveAll(tagIds.stream().map(id -> new UserTag(user.getId(), id)).toList());
        String token = jwtUtil.generate(user.getId(), "USER");
        return new AuthResponse(token, toResponse(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus().intValue() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        String token = jwtUtil.generate(user.getId(), "USER");
        return new AuthResponse(token, toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.email() != null) {
            user.setEmail(blankToNull(request.email()));
        }
        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateInterests(Long userId, UpdateInterestsRequest request) {
        User user = requireUser(userId);
        List<Integer> tagIds = normalizeTagIds(request.tagIds());
        validateTags(tagIds);

        userTagRepository.deleteByUserId(userId);
        userTagRepository.saveAll(tagIds.stream().map(id -> new UserTag(userId, id)).toList());
        return toResponse(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private void validateTags(List<Integer> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在无效的兴趣标签");
        }
    }

    private List<Integer> normalizeTagIds(List<Integer> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        return tagIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private UserResponse toResponse(User user) {
        List<Integer> tagIds = userTagRepository.findByUserId(user.getId()).stream()
                .map(UserTag::getTagId)
                .sorted()
                .toList();
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), "USER", tagIds);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
