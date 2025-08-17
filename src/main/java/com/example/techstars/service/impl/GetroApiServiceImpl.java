package com.example.techstars.service.impl;

import com.example.techstars.client.GetroApiClient;
import com.example.techstars.dto.GetroApiRequest;
import com.example.techstars.dto.GetroJobResponse;
import com.example.techstars.service.GetroApiService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetroApiServiceImpl implements GetroApiService {

    private final GetroApiClient getroApiClient;

    @Override
    public List<GetroJobResponse.JobPayload> fetchAllJobSummaries(String jobFunction) {
        List<GetroJobResponse.JobPayload> allJobs = new ArrayList<>();
        int page = 0;
        boolean hasMoreJobs;

        log.info("Starting to fetch job summaries for function: '{}'", jobFunction);

        do {
            GetroApiRequest request = new GetroApiRequest(new GetroApiRequest.Filters(List.of(jobFunction)), page);
            GetroJobResponse apiResponse = getroApiClient.searchJobsByFunction(request);

            if (apiResponse == null || apiResponse.results() == null || apiResponse.results().jobs().isEmpty()) {
                log.info("API returned no more jobs on page {}. Finishing pagination.", page);
                hasMoreJobs = false;
            } else {
                List<GetroJobResponse.JobPayload> jobsOnPage = apiResponse.results().jobs();
                log.info("Fetched page {}. Found {} jobs. Total count in API: {}", page, jobsOnPage.size(), apiResponse.results().count());

                List<GetroJobResponse.JobPayload> jobsWithDescription = jobsOnPage.stream()
                        .filter(GetroJobResponse.JobPayload::hasDescription)
                        .toList();

                log.info("Filtered jobs on page {}. Jobs with description: {}", page, jobsWithDescription.size());
                allJobs.addAll(jobsWithDescription);

                page++;
                hasMoreJobs = true;
            }

        } while (hasMoreJobs);

        log.info("Finished fetching jobs for function: '{}'. Total jobs found with description: {}", jobFunction, allJobs.size());
        return allJobs;
    }
}