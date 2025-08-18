package com.example.techstars.repository;

import com.example.techstars.model.Organization;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByUrlIn(Set<String> urls);
}