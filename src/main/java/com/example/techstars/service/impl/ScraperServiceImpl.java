package com.example.techstars.service.impl;

import com.example.techstars.dto.GetroJobResponse;
import com.example.techstars.dto.JobScrapedData;
import com.example.techstars.service.GetroApiService;
import com.example.techstars.service.JobDescriptionScraper;
import com.example.techstars.service.PersistenceService;
import com.example.techstars.service.ScraperService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperServiceImpl implements ScraperService {

    private static final String ORG_BASE_URL = "https://jobs.techstars.com/companies/";

    private final GetroApiService getroApiService;
    private final JobDescriptionScraper jobDescriptionScraper;
    private final PersistenceService persistenceService;

    @Override
    public void scrapeJobsByFunction(String jobFunction) {
        try {
            log.info("Step 1/3: Starting API scrape for job list for function: '{}'", jobFunction);
            List<GetroJobResponse.JobPayload> allJobs = getroApiService.fetchAllJobSummaries(jobFunction);

            log.info("Step 2/3: Received {} unique jobs from API. Fetching descriptions...", allJobs.size());

            List<JobScrapedData> scrapedData = allJobs.parallelStream()
                    .map(jobPayload -> toJobScrapedData(jobPayload, jobFunction))
                    .flatMap(Optional::stream)
                    .toList();

            log.info("Step 3/3: Successfully scraped {} descriptions. Persisting data...", scrapedData.size());
            persistenceService.persistScrapedJobsAsync(scrapedData);

        } catch (Exception e) {
            log.error("A critical error occurred during scraping for '{}': {}", jobFunction, e.getMessage(), e);
        }
    }

    private Optional<JobScrapedData> toJobScrapedData(GetroJobResponse.JobPayload payload, String jobFunction) {
        String jobUrl = ORG_BASE_URL + payload.organization().slug() + "/jobs/" + payload.slug();

        return jobDescriptionScraper.fetchJobDescription(jobUrl)
                .map(description ->
                        JobScrapedData.builder()
                                .positionName(payload.title())
                                .jobUrl(jobUrl)
                                .laborFunction(jobFunction)
                                .locationNames(Set.copyOf(payload.locations()))
                                .postedDate(payload.createdAt())
                                .description(description)
                                .orgTitle(payload.organization().name())
                                .orgUrl(ORG_BASE_URL + payload.organization().slug())
                                .orgLogoUrl(payload.organization().logoUrl())
                                .tagNames(Set.copyOf(payload.tags()))
                                .build()
                );
    }
}