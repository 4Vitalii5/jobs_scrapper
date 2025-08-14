package com.example.techstars.repository;

import com.example.techstars.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    boolean existsByJobPageUrl(String jobPageUrl);
    
    List<Job> findByLaborFunction(String laborFunction);
    
    List<Job> findByLocationContainingIgnoreCase(String location);
    
    List<Job> findByPostedDateBetween(Long startDate, Long endDate);
    
    @Query("SELECT DISTINCT j.laborFunction FROM Job j")
    List<String> findAllLaborFunctions();
    
    @Query("SELECT DISTINCT j.location FROM Job j")
    List<String> findAllLocations();
    
    @Query("SELECT COUNT(j) FROM Job j WHERE j.laborFunction = :function")
    long countByLaborFunction(@Param("function") String function);
}