package com.example.techstars.dto;

import com.example.techstars.model.Job;
import com.example.techstars.model.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class JobDto {
    private Long id;
    @Schema(description = "The name of the job position.", example = "Senior Java Developer")
    private String positionName;
    @Schema(description = "URL to the full job page.", example = "https://jobs.techstars.com/jobs/12345")
    private String jobPageUrl;
    @Schema(description = "The labor function or category.", example = "Software Engineering")
    private String laborFunction;
    @Schema(description = "Date when the job was posted (Unix Timestamp).", example = "1672531200")
    private Long postedDate;
    @Schema(description = "Full HTML description of the job.")
    private String description;
    @Schema(description = "General location of the job.", example = "Remote / New York, NY")
    private Set<String> locations;
    @Schema(description = "The organization posting the job.")
    private OrganizationDto organization;
    @Schema(description = "A set of tags associated with the job.")
    private Set<String> tags;
} 