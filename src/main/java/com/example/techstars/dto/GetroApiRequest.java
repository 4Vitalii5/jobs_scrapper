package com.example.techstars.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GetroApiRequest(
        Filters filters,
        int from
) {
    public record Filters(
            @JsonProperty("job_functions") List<String> jobFunctions
    ) {}
}