package com.example.techstars.dto;

import java.util.Set;

public record JobScrapedData(
        String positionName,
        String jobPageUrl,
        String laborFunction,
        Set<String> locationNames,
        long postedDate,
        String description,
        String orgTitle,
        String orgUrl,
        String orgLogoUrl,
        Set<String> tagNames
) {}