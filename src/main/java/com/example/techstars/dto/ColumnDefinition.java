package com.example.techstars.dto;

import java.util.function.Function;
import lombok.Builder;

@Builder
public record ColumnDefinition(String header, Function<JobDto, Object> extractor) {
    }