package com.example.techstars.service.impl;

import com.example.techstars.client.GetroApiClient;
import com.example.techstars.dto.GetroApiRequest;
import com.example.techstars.dto.GetroJobResponse;
import com.example.techstars.service.GetroApiService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetroApiServiceImpl implements GetroApiService {

    private final GetroApiClient getroApiClient;

    @Override
    public List<GetroJobResponse.JobPayload> fetchAllJobSummaries(String jobFunction) {
        List<GetroJobResponse.JobPayload> allJobs = new ArrayList<>();
        int page = 0;
        boolean hasMoreJobs;

        do {
            GetroApiRequest.Filters filters = GetroApiRequest.Filters.builder()
                    .jobFunctions(List.of(jobFunction))
                    .build();

            GetroApiRequest request = GetroApiRequest.builder()
                    .page(page)
                    .filters(filters)
                    .build();

            GetroJobResponse apiResponse = getroApiClient.searchJobsByFunction(request);

            if (apiResponse == null || apiResponse.results() == null
                    || apiResponse.results().jobs().isEmpty()) {
                hasMoreJobs = false;
            } else {
                List<GetroJobResponse.JobPayload> jobsOnPage = apiResponse.results().jobs();

                List<GetroJobResponse.JobPayload> jobsWithDescription = jobsOnPage.stream()
                        .filter(GetroJobResponse.JobPayload::hasDescription)
                        .toList();

                allJobs.addAll(jobsWithDescription);

                page++;
                hasMoreJobs = true;
            }

        } while (hasMoreJobs);

        return allJobs;
    }
}