package com.example.techstars.service;

import com.example.techstars.dto.GetroJobResponse;
import java.util.List;

public interface GetroApiService {
    List<GetroJobResponse.JobPayload> fetchAllJobSummaries(String jobFunction);
}