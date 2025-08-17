package com.example.techstars.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GetroJobResponse(Results results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(List<JobPayload> jobs, int count) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobPayload(
            long id,
            String slug,
            String title,
            String url,
            @JsonProperty("has_description") boolean hasDescription,
            @JsonProperty("created_at") long createdAt,
            String description,
            List<String> locations,
            OrganizationPayload organization,
            @JsonProperty("skills") List<String> tags
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrganizationPayload(
            String name,
            @JsonProperty("logo_url") String logoUrl,
            String slug
    ) {}
}