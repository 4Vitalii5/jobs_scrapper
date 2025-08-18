package com.example.techstars.service;

import java.util.Optional;

public interface JobDescriptionScraper {
    Optional<String> fetchJobDescription(String jobUrl);
}