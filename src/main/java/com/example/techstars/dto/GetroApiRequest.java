package com.example.techstars.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record GetroApiRequest(
        Filters filters,
        int page
) {
    @Builder
    public record Filters(
            @JsonProperty("job_functions") List<String> jobFunctions
    ) {}
}