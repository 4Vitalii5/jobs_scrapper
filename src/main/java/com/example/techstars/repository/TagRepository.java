package com.example.techstars.repository;

import com.example.techstars.model.Tag;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameIn(Set<String> names);
}