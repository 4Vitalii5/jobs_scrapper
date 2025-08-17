package com.example.techstars.service.impl;

import com.example.techstars.client.GetroApiClient;
import com.example.techstars.dto.GetroApiRequest;
import com.example.techstars.dto.GetroJobResponse;
import com.example.techstars.dto.JobScrapedData;
import com.example.techstars.model.Job;
import com.example.techstars.model.Location;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.LocationRepository;
import com.example.techstars.repository.OrganizationRepository;
import com.example.techstars.repository.TagRepository;
import com.example.techstars.service.ScraperService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperServiceImpl implements ScraperService {
    @Value("${scraper.request-delay:500}")
    private long requestDelayMs;

    @Value("${scraper.random-delay:true}")
    private boolean useRandomDelay;

    private static final String ORG_BASE_URL = "https://jobs.techstars.com/companies/";
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0"
    );

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final TagRepository tagRepository;
    private final LocationRepository locationRepository;
    private final GetroApiClient getroApiClient;

    @Override
    public void scrapeJobsByFunction(String jobFunction) {
        try {
            log.info("Step 1/3: Starting API scrape for job list for function: '{}'", jobFunction);
            List<GetroJobResponse.JobPayload> allJobs = fetchAllJobSummaries(jobFunction);

            log.info("Step 2/3: Received {} unique jobs from API. Fetching descriptions using Jsoup...", allJobs.size());

            List<JobScrapedData> scrapedData = allJobs.parallelStream()
                    .filter(GetroJobResponse.JobPayload::hasDescription)
                    .map(jobPayload -> toJobScrapedData(jobPayload, jobFunction))
                    .flatMap(Optional::stream)
                    .toList();

            log.info("Step 3/3: Successfully scraped {} descriptions. Persisting data...", scrapedData.size());
            persistScrapedJobsAsync(scrapedData);

        } catch (Exception e) {
            log.error("A critical error occurred during scraping for '{}': {}", jobFunction, e.getMessage(), e);
        }
    }

    @Async("scrapingTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistScrapedJobsAsync(List<JobScrapedData> jobsData) {
        persistScrapedJobs(jobsData);
    }

    private List<GetroJobResponse.JobPayload> fetchAllJobSummaries(String jobFunction) {
        List<GetroJobResponse.JobPayload> allJobs = new ArrayList<>();
        int from = 0;
        int totalJobs;

        do {
            GetroApiRequest request = new GetroApiRequest(new GetroApiRequest.Filters(List.of(jobFunction)), from);
            GetroJobResponse apiResponse = getroApiClient.searchJobsByFunction(request);

            if (apiResponse == null || apiResponse.results() == null
                    || apiResponse.results().jobs() == null
                    || apiResponse.results().jobs().isEmpty()) {
                log.info("API returned no more jobs. Finishing pagination.");
                break;
            }

            List<GetroJobResponse.JobPayload> jobsOnPage = apiResponse.results().jobs();
            allJobs.addAll(jobsOnPage);
            totalJobs = apiResponse.results().count();
            from += jobsOnPage.size();

            log.info("Fetched {} jobs of {}. Continuing...", allJobs.size(), totalJobs);

        } while (from < totalJobs);

        return allJobs;
    }

    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private String fetchJobDescription(String techstarsJobUrl) {
        try {
            Document doc = Jsoup.connect(techstarsJobUrl)
                    .userAgent(getRandomUserAgent())
                    .timeout(30000)
                    .get();
            Element descriptionElement = doc.select("div[data-testid='careerPage'], div[class*='job-description']").first();
            return descriptionElement != null ? descriptionElement.html() : "";
        } catch (IOException e) {
            log.warn("Could not fetch description from URL {}: {}", techstarsJobUrl, e.getMessage());
            return "";
        }
    }

    private String getRandomUserAgent() {
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0"
        };
        return userAgents[(int) (Math.random() * userAgents.length)];
    }

    private Optional<JobScrapedData> toJobScrapedData(GetroJobResponse.JobPayload payload, String jobFunction) {
        String jobUrl = ORG_BASE_URL + payload.organization().slug() + "/jobs/" + payload.slug();
        String description = fetchJobDescription(jobUrl);

        if (StringUtils.hasText(description)) {
            return Optional.of(new JobScrapedData(
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
        log.warn("Description is empty for job '{}' at {}, skipping.", payload.title(), jobUrl);
        return Optional.empty();
    }

    @Async
    @Transactional
    public void persistScrapedJobs(List<JobScrapedData> jobsData) {
        if (jobsData.isEmpty()) {
            log.info("No new jobs to persist.");
            return;
        }

        Set<String> existingUrls = new HashSet<>(jobRepository.findAllJobUrls());

        List<JobScrapedData> newJobsData = jobsData.stream()
                .filter(data -> !existingUrls.contains(data.jobPageUrl()))
                .toList();

        if (newJobsData.isEmpty()) {
            log.info("No new jobs found to persist.");
            return;
        }

        log.info("Persisting {} new jobs...", newJobsData.size());

        Map<String, Organization> organizations = findOrCreateOrganizations(newJobsData);
        Map<String, Tag> tags = findOrCreateTags(newJobsData);
        Map<String, Location> locations = findOrCreateLocations(newJobsData);

        List<Job> jobsToSave = newJobsData.stream().map(data -> {
            Organization org = organizations.get(data.orgUrl());
            Set<Tag> jobTags = data.tagNames().stream().map(tags::get).collect(Collectors.toSet());
            Set<Location> jobLocations = data.locationNames().stream().map(locations::get).collect(Collectors.toSet());

            return Job.builder()
                    .positionName(data.positionName())
                    .jobPageUrl(data.jobPageUrl())
                    .laborFunction(data.laborFunction())
                    .locations(jobLocations)
                    .postedDate(data.postedDate())
                    .description(data.description())
                    .organization(org)
                    .tags(jobTags)
                    .build();
        }).toList();

        jobRepository.saveAll(jobsToSave);
        log.info("Successfully saved {} new jobs to the database.", jobsToSave.size());
    }

    private Map<String, Location> findOrCreateLocations(List<JobScrapedData> jobsData) {
        Set<String> locationNames = jobsData.stream()
                .flatMap(data -> data.locationNames().stream())
                .collect(Collectors.toSet());

        Map<String, Location> existingLocations = locationRepository.findByNameIn(locationNames).stream()
                .collect(Collectors.toMap(Location::getName, Function.identity()));

        List<Location> newLocationsToSave = locationNames.stream()
                .filter(name -> !existingLocations.containsKey(name))
                .map(name -> Location.builder().name(name).build())
                .toList();

        if (!newLocationsToSave.isEmpty()) {
            locationRepository.saveAll(newLocationsToSave)
                    .forEach(loc -> existingLocations.put(loc.getName(), loc));
        }
        return existingLocations;
    }

    private Map<String, Organization> findOrCreateOrganizations(List<JobScrapedData> jobsData) {
        Set<String> orgUrls = jobsData.stream().map(JobScrapedData::orgUrl).collect(Collectors.toSet());
        Map<String, Organization> existingOrgs = organizationRepository.findByUrlIn(orgUrls).stream()
                .collect(Collectors.toMap(Organization::getUrl, Function.identity()));

        Map<String, JobScrapedData> dataByUrl = jobsData.stream()
                .collect(Collectors.toMap(JobScrapedData::orgUrl, Function.identity(), (d1, d2) -> d1));

        List<Organization> newOrgsToSave = dataByUrl.values().stream()
                .filter(data -> !existingOrgs.containsKey(data.orgUrl()))
                .map(data -> Organization.builder()
                        .title(data.orgTitle())
                        .url(data.orgUrl())
                        .logoUrl(data.orgLogoUrl())
                        .build())
                .toList();

        if (!newOrgsToSave.isEmpty()) {
            organizationRepository.saveAll(newOrgsToSave).forEach(org -> existingOrgs.put(org.getUrl(), org));
        }
        return existingOrgs;
    }

    private Map<String, Tag> findOrCreateTags(List<JobScrapedData> jobsData) {
        Set<String> tagNames = jobsData.stream()
                .flatMap(data -> data.tagNames().stream())
                .collect(Collectors.toSet());

        Map<String, Tag> existingTags = tagRepository.findByNameIn(tagNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> newTagsToSave = tagNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(name -> Tag.builder().name(name).build())
                .toList();

        if (!newTagsToSave.isEmpty()) {
            tagRepository.saveAll(newTagsToSave).forEach(tag -> existingTags.put(tag.getName(), tag));
        }
        return existingTags;
    }
}