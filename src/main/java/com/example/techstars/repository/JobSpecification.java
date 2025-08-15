package com.example.techstars.repository;

import com.example.techstars.model.Job;
import com.example.techstars.model.Location;
import com.example.techstars.model.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class JobSpecification {

    public static Specification<Job> findByCriteria(String location, String jobFunction, List<String> tags) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Оновлена логіка для фільтрації за локацією
            if (StringUtils.hasText(location)) {
                // Робимо JOIN з таблицею локацій
                Join<Job, Location> locationJoin = root.join("locations");
                // Застосовуємо умову LIKE до поля 'name' в таблиці локацій
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(locationJoin.get("name")),
                        "%" + location.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(jobFunction)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("laborFunction")), jobFunction.toLowerCase()));
            }

            if (tags != null && !tags.isEmpty()) {
                Join<Job, Tag> tagJoin = root.join("tags");
                predicates.add(tagJoin.get("name").in(tags));
            }

            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
} 