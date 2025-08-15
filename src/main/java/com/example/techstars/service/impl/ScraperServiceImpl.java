package com.example.techstars.service.impl;

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
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperServiceImpl implements ScraperService {

    private static final String API_SEARCH_URL = "https://api.getro.com/api/v2/collections/89/search/jobs";
    private static final String ORG_BASE_URL = "https://jobs.techstars.com/companies/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final TagRepository tagRepository;
    private final LocationRepository locationRepository;
    private final RestTemplate restTemplate;

    @Override
    public void scrapeJobsByFunction(String jobFunction) {
        try {
            log.info("Step 1/3: Starting API scrape for job function: {}", jobFunction);
            GetroJobResponse apiResponse = fetchJobsFromApi(jobFunction);

            if (apiResponse == null || apiResponse.results() == null
                    || apiResponse.results().jobs() == null) {
                log.warn("API response was empty or invalid for function: {}", jobFunction);
                return;
            }
            log.info("Step 2/3: Received {} jobs from API. Fetching descriptions from jobs.techstars.com...", apiResponse.results().count());

            List<JobScrapedData> scrapedData = apiResponse.results().jobs().parallelStream()
                    .filter(GetroJobResponse.JobPayload::hasDescription)
                    .map(jobPayload -> toJobScrapedData(jobPayload, jobFunction))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            log.info("Step 3/3: Persisting data to the database asynchronously...");
            persistScrapedJobs(scrapedData);

        } catch (Exception e) {
            log.error("A critical error occurred during scraping for '{}': {}", jobFunction, e.getMessage(), e);
        }
    }

    private GetroJobResponse fetchJobsFromApi(String jobFunction) {
        HttpHeaders headers = createHeaders();
        String requestBody = String.format("{\"query\":\"\",\"filters\":{\"job_functions\":[\"%s\"]},\"from\":0,\"size\":1000}", jobFunction);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(API_SEARCH_URL, entity, GetroJobResponse.class);
    }

    private String fetchJobDescription(String techstarsJobUrl) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", USER_AGENT);
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            headers.put("Referer", "https://jobs.techstars.com/");

            Document doc = Jsoup.connect(techstarsJobUrl).headers(headers).timeout(30000).get();

            Element descriptionElement = doc.select("div[data-testid='careerPage'], div[class*='job-description-container']").first();

            return descriptionElement != null ? descriptionElement.html() : "";

        } catch (IOException e) {
            log.warn("Could not fetch description from URL {}: {}", techstarsJobUrl, e.getMessage());
            return "";
        }
    }

    private Optional<JobScrapedData> toJobScrapedData(GetroJobResponse.JobPayload payload, String jobFunction) {
        // Генеруємо "нативну" URL на jobs.techstars.com
        String techstarsJobUrl =
                ORG_BASE_URL + payload.organization().slug() + "/jobs/" + payload.slug();

        // Йдемо за описом саме на цю згенеровану сторінку
        String description = fetchJobDescription(techstarsJobUrl);

        if (description == null || description.isBlank()) {
            log.warn("Description is empty for job '{}' at {}, skipping.", payload.title(), techstarsJobUrl);
            return Optional.empty();
        }

        return Optional.of(new JobScrapedData(
                payload.title(),
                techstarsJobUrl,
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

        // Створюємо мапу URL -> DTO, щоб уникнути повторного пошуку
        Map<String, JobScrapedData> dataByUrl = jobsData.stream()
                .collect(Collectors.toMap(JobScrapedData::orgUrl, Function.identity(), (d1, d2) -> d1));

        List<Organization> newOrgsToSave = dataByUrl.values().stream()
                .filter(data -> !existingOrgs.containsKey(data.orgUrl()))
                .map(data -> Organization.builder()
                        .title(data.orgTitle())
                        .url(data.orgUrl())
                        .logoUrl(data.orgLogoUrl()) // <-- Зберігаємо логотип при створенні
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

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", USER_AGENT);
        return headers;
    }
}