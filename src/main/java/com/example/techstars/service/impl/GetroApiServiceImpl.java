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
        Set<String> seenSlugs = new HashSet<>();
        int totalSize = 0;

        for (int page = 0; ; page++) {
            GetroApiRequest request = new GetroApiRequest(new GetroApiRequest.Filters(List.of(jobFunction)), page);
            GetroJobResponse apiResponse = getroApiClient.searchJobsByFunction(request);

            totalSize = apiResponse.results().count();

            log.info("Total size of jobs {}", totalSize);

            if (apiResponse == null || apiResponse.results() == null
                    || apiResponse.results().jobs() == null
                    || apiResponse.results().jobs().isEmpty()) {
                log.info("API returned no more jobs on page {}. Finishing pagination.", page);
                break;
            }

            List<GetroJobResponse.JobPayload> jobsOnPage = apiResponse.results().jobs();

            log.info("Fetched page {}. Found {} jobs",
                    page, jobsOnPage.size());

            List<GetroJobResponse.JobPayload> jobsWithDescription = jobsOnPage.stream()
                    .filter(GetroJobResponse.JobPayload::hasDescription)
                    .toList();

            log.info("Jobs that has description :{}", jobsWithDescription.size());

            allJobs.addAll(jobsWithDescription);
        }

        return allJobs;
    }
}