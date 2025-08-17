package com.example.techstars.service.impl;

import com.example.techstars.dto.JobScrapedData;
import com.example.techstars.model.Job;
import com.example.techstars.model.Location;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.LocationRepository;
import com.example.techstars.repository.OrganizationRepository;
import com.example.techstars.repository.TagRepository;
import com.example.techstars.service.PersistenceService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenceServiceImpl implements PersistenceService {

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final TagRepository tagRepository;
    private final LocationRepository locationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void persistScrapedJobsAsync(List<JobScrapedData> jobsData) {
        if (jobsData.isEmpty()) {
            log.info("No new jobs to persist.");
            return;
        }

        Set<String> incomingUrls = jobsData.stream()
                .map(JobScrapedData::jobUrl)
                .collect(Collectors.toSet());

        Set<String> existingUrls = jobRepository.findExistingUrls(incomingUrls);

        List<JobScrapedData> newJobsData = jobsData.stream()
                .filter(data -> !existingUrls.contains(data.jobUrl()))
                .toList();

        if (newJobsData.isEmpty()) {
            log.info("No new jobs to persist after filtering existing URLs.");
            return;
        }

        log.info("Found {} new jobs to persist...", newJobsData.size());

        Map<String, Organization> organizations = findOrCreateOrganizations(newJobsData);
        Map<String, Tag> tags = findOrCreateTags(newJobsData);
        Map<String, Location> locations = findOrCreateLocations(newJobsData);

        List<Job> jobsToSave = newJobsData.stream().map(data -> {
            Organization org = organizations.get(data.orgUrl());
            Set<Tag> jobTags = data.tagNames().stream().map(tags::get).collect(Collectors.toSet());
            Set<Location> jobLocations = data.locationNames().stream().map(locations::get).collect(Collectors.toSet());

            return Job.builder()
                    .positionName(data.positionName())
                    .jobPageUrl(data.jobUrl())
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
        Set<String> allLocationNames = jobsData.stream()
                .flatMap(data -> data.locationNames().stream())
                .collect(Collectors.toSet());

        if (allLocationNames.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Location> existingLocations = locationRepository.findByNameIn(allLocationNames);
        Map<String, Location> locationMap = existingLocations.stream()
                .collect(Collectors.toMap(Location::getName, Function.identity()));

        List<Location> newLocationsToCreate = allLocationNames.stream()
                .filter(name -> !locationMap.containsKey(name))
                .map(name -> Location.builder().name(name).build())
                .toList();

        if (!newLocationsToCreate.isEmpty()) {
            List<Location> savedLocations = locationRepository.saveAll(newLocationsToCreate);
            savedLocations.forEach(loc -> locationMap.put(loc.getName(), loc));
        }

        return locationMap;
    }

    private Map<String, Tag> findOrCreateTags(List<JobScrapedData> jobsData) {
        Set<String> allTagNames = jobsData.stream()
                .flatMap(data -> data.tagNames().stream())
                .collect(Collectors.toSet());

        if (allTagNames.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tag> existingTags = tagRepository.findByNameIn(allTagNames);
        Map<String, Tag> tagMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> newTagsToCreate = allTagNames.stream()
                .filter(name -> !tagMap.containsKey(name))
                .map(name -> Tag.builder().name(name).build())
                .toList();

        if (!newTagsToCreate.isEmpty()) {
            List<Tag> savedTags = tagRepository.saveAll(newTagsToCreate);
            savedTags.forEach(tag -> tagMap.put(tag.getName(), tag));
        }

        return tagMap;
    }

    private Map<String, Organization> findOrCreateOrganizations(List<JobScrapedData> jobsData) {
        Map<String, JobScrapedData> uniqueOrgDataByUrl = jobsData.stream()
                .collect(Collectors.toMap(JobScrapedData::orgUrl, Function.identity(), (first, second) -> first));

        if (uniqueOrgDataByUrl.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> allOrgUrls = uniqueOrgDataByUrl.keySet();

        List<Organization> existingOrgs = organizationRepository.findByUrlIn(allOrgUrls);
        Map<String, Organization> orgMap = existingOrgs.stream()
                .collect(Collectors.toMap(Organization::getUrl, Function.identity()));

        List<Organization> newOrgsToCreate = allOrgUrls.stream()
                .filter(url -> !orgMap.containsKey(url))
                .map(url -> {
                    JobScrapedData data = uniqueOrgDataByUrl.get(url);
                    return Organization.builder()
                            .title(data.orgTitle())
                            .url(data.orgUrl())
                            .logoUrl(data.orgLogoUrl())
                            .build();
                })
                .toList();

        if (!newOrgsToCreate.isEmpty()) {
            List<Organization> savedOrgs = organizationRepository.saveAll(newOrgsToCreate);
            savedOrgs.forEach(org -> orgMap.put(org.getUrl(), org));
        }

        return orgMap;
    }
}