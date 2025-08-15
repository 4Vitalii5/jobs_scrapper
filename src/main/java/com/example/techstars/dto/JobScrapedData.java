package com.example.techstars.dto;

import java.util.Set;

public record JobScrapedData(
    String positionName,
    String jobPageUrl,
    String logoUrl,
    String laborFunction,
    String location,
    long postedDate,
    String description,
    String orgTitle,
    String orgUrl,
    Set<String> tagNames
) {}