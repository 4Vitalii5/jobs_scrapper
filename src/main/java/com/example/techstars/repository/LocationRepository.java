package com.example.techstars.repository;

import com.example.techstars.model.Location;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByNameIn(Set<String> names);
}