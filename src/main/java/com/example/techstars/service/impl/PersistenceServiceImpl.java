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
import org.springframework.data.jpa.repository.JpaRepository;
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

    private <T> Map<String, T> findOrCreateByName(
            Set<String> names,
            Function<Set<String>, List<T>> finder,
            Function<String, T> creator,
            Function<T, String> nameExtractor,
            JpaRepository<T, ?> repository
    ) {
        if (names.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, T> existingEntities = finder.apply(names).stream()
                .collect(Collectors.toMap(nameExtractor, Function.identity()));

        List<T> newEntitiesToSave = names.stream()
                .filter(name -> !existingEntities.containsKey(name))
                .map(creator)
                .toList();

        if (!newEntitiesToSave.isEmpty()) {
            repository.saveAll(newEntitiesToSave)
                    .forEach(entity -> existingEntities.put(nameExtractor.apply(entity), entity));
        }
        return existingEntities;
    }

    private Map<String, Location> findOrCreateLocations(List<JobScrapedData> jobsData) {
        Set<String> locationNames = jobsData.stream()
                .flatMap(data -> data.locationNames().stream())
                .collect(Collectors.toSet());

        return findOrCreateByName(
                locationNames,
                locationRepository::findByNameIn,
                name -> Location.builder().name(name).build(),
                Location::getName,
                locationRepository
        );
    }

    private Map<String, Tag> findOrCreateTags(List<JobScrapedData> jobsData) {
        Set<String> tagNames = jobsData.stream()
                .flatMap(data -> data.tagNames().stream())
                .collect(Collectors.toSet());

        return findOrCreateByName(
                tagNames,
                tagRepository::findByNameIn,
                name -> Tag.builder().name(name).build(),
                Tag::getName,
                tagRepository
        );
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
}