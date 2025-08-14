package com.example.techstars.service;

import com.example.techstars.dto.JobDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {
    Page<JobDto> getAllJobs(Pageable pageable);
    JobDto getJobById(Long id);
    List<JobDto> getJobsByFunction(String laborFunction);
    List<JobDto> getJobsByLocation(String location);
    List<JobDto> getJobsByDateRange(Long startDate, Long endDate);
    List<String> getAllLaborFunctions();
    List<String> getAllLocations();
    long getJobCountByFunction(String function);
}