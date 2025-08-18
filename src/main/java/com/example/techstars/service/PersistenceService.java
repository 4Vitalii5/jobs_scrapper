package com.example.techstars.service;

import com.example.techstars.dto.JobScrapedData;
import java.util.List;

public interface PersistenceService {
    void persistScrapedJobsAsync(List<JobScrapedData> jobsData);
}