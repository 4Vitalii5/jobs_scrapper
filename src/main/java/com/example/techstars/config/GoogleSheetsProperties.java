package com.example.techstars.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "google.sheets")
public class GoogleSheetsProperties {

    private String spreadsheetId;

    private final Credentials credentials = new Credentials();

    @Data
    public static class Credentials {
        private String content;
    }
}