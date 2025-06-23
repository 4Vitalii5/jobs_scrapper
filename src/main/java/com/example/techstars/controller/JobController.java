package com.example.techstars.controller;

import com.example.techstars.dto.JobDTO;
import com.example.techstars.dto.OrganizationDTO;
import com.example.techstars.dto.TagDTO;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.JobSpecification;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobRepository jobRepository;

    @GetMapping
    public Page<JobDTO> getJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobFunction,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        List<String> tagList = tags != null
                && !tags.isEmpty() ? List.of(tags.split(",")) : Collections.emptyList();

        Specification<Job> spec = JobSpecification.findByCriteria(location, jobFunction, tagList);

        Page<Job> jobsPage = jobRepository.findAll(spec, pageable);

        return jobsPage.map(this::convertToDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private JobDTO convertToDto(Job job) {
        OrganizationDTO orgDto = Optional.ofNullable(job.getOrganization())
                .map(org -> OrganizationDTO.builder()
                        .id(org.getId())
                        .title(org.getTitle())
                        .url(org.getUrl())
                        .build())
                .orElse(null);

        List<TagDTO> tagDtos = Optional.ofNullable(job.getTags()).orElse(Collections.emptySet()).stream()
                .map(tag -> TagDTO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .build())
                .toList();

        return JobDTO.builder()
                .id(job.getId())
                .positionName(job.getPositionName())
                .jobPageUrl(job.getJobPageUrl())
                .logoUrl(job.getLogoUrl())
                .laborFunction(job.getLaborFunction())
                .postedDate(job.getPostedDate())
                .description(job.getDescription())
                .location(job.getLocation())
                .organization(orgDto)
                .tags(tagDtos)
                .build();
    }
} 