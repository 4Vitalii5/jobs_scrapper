package com.example.techstars.repository;

import com.example.techstars.model.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByUrl(String orgUrl);
}