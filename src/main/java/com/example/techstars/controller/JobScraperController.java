package com.example.techstars.controller;

import com.example.techstars.service.ScraperService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scrape")
@RequiredArgsConstructor
@Tag(name = "Scraper Controller", description = "Endpoints to initiate job scraping")
public class JobScraperController {
    private final ScraperService scraperService;

    @PostMapping("/{jobFunction}")
    public ResponseEntity<String> scrapeJobs(@PathVariable String jobFunction) {
        int count = scraperService.scrapeJobsByFunction(jobFunction);
        return ResponseEntity.ok(
                "Scraped and saved " + count + " jobs for function: " + jobFunction);
    }
} 