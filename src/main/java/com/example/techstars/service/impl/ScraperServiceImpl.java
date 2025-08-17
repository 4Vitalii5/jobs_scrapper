package com.example.techstars.service.impl;

import com.example.techstars.dto.GetroJobResponse;
import com.example.techstars.dto.JobScrapedData;
import com.example.techstars.service.GetroApiService;
import com.example.techstars.service.JobDescriptionScraper;
import com.example.techstars.service.PersistenceService;
import com.example.techstars.service.ScraperService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
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

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private Optional<JobScrapedData> toJobScrapedData(GetroJobResponse.JobPayload payload, String jobFunction) {
        String jobUrl = ORG_BASE_URL + payload.organization().slug() + "/jobs/" + payload.slug();

        return jobDescriptionScraper.fetchJobDescription(jobUrl)
                .map(description -> new JobScrapedData(
                        payload.title(),
                        jobUrl,
                        jobFunction,
                        new HashSet<>(payload.locations()),
                        payload.createdAt(),
                        description,
                        payload.organization().name(),
                        ORG_BASE_URL + payload.organization().slug(),
                        payload.organization().logoUrl(),
                        new HashSet<>(payload.tags())
                ));
    }
}