package com.campuspulse.server.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.ActivityTag;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class ActivitySpecifications {

    private ActivitySpecifications() {
    }

    public static Specification<Activity> publishedFilter(String keyword,
                                                          Integer tagId,
                                                          String type,
                                                          LocalDate date) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title").as(String.class)), like),
                        cb.like(cb.lower(root.get("description").as(String.class)), like),
                        cb.like(cb.lower(root.get("location").as(String.class)), like),
                        cb.like(cb.lower(root.get("type").as(String.class)), like)));
            }

            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type.trim().toUpperCase(Locale.ROOT)));
            }

            if (date != null) {
                LocalDateTime from = date.atStartOfDay();
                LocalDateTime to = date.plusDays(1).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
                predicates.add(cb.lessThan(root.get("startTime"), to));
            }

            if (tagId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ActivityTag> activityTag = subquery.from(ActivityTag.class);
                subquery.select(activityTag.get("id").get("activityId"));
                subquery.where(cb.equal(activityTag.get("id").get("tagId"), tagId));
                CriteriaBuilder.In<Long> in = cb.in(root.get("id"));
                in.value(subquery);
                predicates.add(in);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
