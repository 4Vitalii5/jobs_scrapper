package com.example.techstars.dto;

import com.example.techstars.model.Job;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
    private Long id;
    private String positionName;
    private String jobPageUrl;
    private String logoUrl;
    private String laborFunction;
    private Long postedDate;
    private String description;
    private String location;
    private String address;
    private OrganizationDTO organization;
    private Set<String> tags;

    public static JobDTO fromEntity(Job job) {
        return JobDTO.builder()
                .id(job.getId())
                .positionName(job.getPositionName())
                .jobPageUrl(job.getJobPageUrl())
                .logoUrl(job.getLogoUrl())
                .laborFunction(job.getLaborFunction())
                .postedDate(job.getPostedDate())
                .description(job.getDescription())
                .location(job.getLocation())
                .address(job.getAddress())
                .organization(OrganizationDTO.fromEntity(job.getOrganization()))
                .tags(job.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
} 