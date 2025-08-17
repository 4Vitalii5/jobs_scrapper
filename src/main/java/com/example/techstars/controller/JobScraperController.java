package com.example.techstars.controller;

import com.example.techstars.model.Function;
import com.example.techstars.service.ScraperService;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ResponseEntity<String> scrapeJobs(@Parameter(description = "Job function to scrape", required = true)
                                                 @PathVariable Function jobFunction) {
        scraperService.scrapeJobsByFunction(jobFunction.getLabel());
        return ResponseEntity.ok(
                "Scraping process for job function '" + jobFunction + "' started in the background.");
    }
}
