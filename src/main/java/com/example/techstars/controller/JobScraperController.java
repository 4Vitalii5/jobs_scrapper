package com.example.techstars.controller;

import com.example.techstars.service.JobScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scrape")
@RequiredArgsConstructor
public class JobScraperController {
    private final JobScraperService jobScraperService;

    @PostMapping("/{jobFunction}")
    public ResponseEntity<String> scrapeJobs(@PathVariable String jobFunction) {
        int count = jobScraperService.scrapeJobsByFunction(jobFunction);
        return ResponseEntity.ok(
                "Scraped and saved " + count + " jobs for function: " + jobFunction);
    }
} 