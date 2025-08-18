package com.example.techstars.service.impl;

import com.example.techstars.service.JobDescriptionScraper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JobDescriptionScraperImpl implements JobDescriptionScraper {

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0"
    );

    @Override
    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 1000))
    public Optional<String> fetchJobDescription(String jobUrl) {
        try {
            Document doc = Jsoup.connect(jobUrl)
                    .userAgent(getRandomUserAgent())
                    .timeout(30000)
                    .get();
            Element descriptionElement = doc.select("div[data-testid='careerPage'], div[class*='job-description']").first();

            if (descriptionElement != null) {
                return Optional.of(descriptionElement.html());
            }
        } catch (IOException e) {
            log.warn("Could not fetch description from URL {}: {}", jobUrl, e.getMessage());
        }
        return Optional.empty();
    }

    private String getRandomUserAgent() {
        return USER_AGENTS.get((int) (Math.random() * USER_AGENTS.size()));
    }
}