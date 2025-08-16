package com.example.techstars.repository;

import com.example.techstars.model.Job;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @EntityGraph(attributePaths = {"locations", "tags", "organization"})
    List<Job> findByLaborFunction(String laborFunction);

    @Query("SELECT j FROM Job j JOIN j.locations l WHERE lower(l.name) LIKE lower(concat('%', :location, '%'))")
    List<Job> findByLocationContainingIgnoreCase(@Param("location") String location);

    List<Job> findByPostedDateBetween(Long startDate, Long endDate);

    @Query("SELECT DISTINCT j.laborFunction FROM Job j")
    List<String> findAllLaborFunctions();

    @Query("SELECT DISTINCT l.name FROM Job j JOIN j.locations l ORDER BY l.name")
    List<String> findAllLocations();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.laborFunction = :function")
    long countByLaborFunction(@Param("function") String function);

    @Query("SELECT j.jobPageUrl FROM Job j")
    List<String> findAllJobUrls();
}