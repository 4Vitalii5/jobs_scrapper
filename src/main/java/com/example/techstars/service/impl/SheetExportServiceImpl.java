package com.example.techstars.service.impl;

import com.example.techstars.config.GoogleSheetsProperties;
import com.example.techstars.dto.JobDto;
import com.example.techstars.mapper.JobMapper;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.SheetExportService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetExportServiceImpl implements SheetExportService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    private final JobMapper jobMapper;
    private final GoogleSheetsProperties sheetsProperties;
    private final JobRepository jobRepository;

    @Override
    public boolean exportJobsToGoogleSheets(String laborFunction) {
        try {
            List<Job> jobs = jobRepository.findByLaborFunction(laborFunction);
            if (jobs.isEmpty()) {
                log.warn("No jobs found for function: {}", laborFunction);
                return false;
            }

            List<JobDto> jobDtos = jobs.stream()
                    .map(jobMapper::toDto)
                    .collect(Collectors.toList());

            return uploadToGoogleSheets(jobDtos, laborFunction);
        } catch (Exception e) {
            log.error("Error exporting jobs to Google Sheets: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean uploadToGoogleSheets(List<JobDto> jobs, String sheetName) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(httpTransport);

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("Techstars Job Scraper")
                    .build();

            List<List<Object>> data = prepareDataForUpload(jobs);

            ValueRange body = new ValueRange().setValues(data);

            String range = sheetName + "!A1";
            service.spreadsheets().values()
                    .clear(sheetsProperties.getSpreadsheetId(),
                            sheetName + "!A:Z", new ClearValuesRequest())
                    .execute();

            service.spreadsheets().values()
                    .update(sheetsProperties.getSpreadsheetId(), range, body)
                    .setValueInputOption("RAW")
                    .execute();

            log.info("Successfully uploaded {} jobs to Google Sheets", jobs.size());
            return true;
        } catch (Exception e) {
            log.error("Error uploading to Google Sheets: {}", e.getMessage(), e);
            return false;
        }
    }

    private List<List<Object>> prepareDataForUpload(List<JobDto> jobs) {
        List<List<Object>> data = new ArrayList<>();

        List<Object> header = List.of(
                "Position Name", "Job Page URL", "Logo URL", "Labor Function",
                "Posted Date", "Location", "Address", "Organization Title",
                "Organization URL", "Tags", "Description"
        );
        data.add(header);

        for (JobDto job : jobs) {
            List<Object> row = List.of(
                    job.getPositionName(),
                    job.getJobPageUrl(),
                    job.getLogoUrl(),
                    job.getLaborFunction(),
                    formatDate(job.getPostedDate()),
                    job.getLocation(),
                    job.getAddress(),
                    job.getOrganization().getTitle(),
                    job.getOrganization().getUrl(),
                    String.join(", ", job.getTags()),
                    job.getDescription()
            );
            data.add(row);
        }

        return data;
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "N/A";
        }
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private Credential getCredentials(NetHttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String credentialsContent = sheetsProperties.getCredentials().getContent();

        if (StringUtils.isBlank(credentialsContent)) {
            throw new IllegalStateException("Google Sheets credentials content not configured. Please set GOOGLE_CREDENTIALS_JSON environment variable.");
        }

        try (InputStream inputStream = new ByteArrayInputStream(credentialsContent.getBytes(StandardCharsets.UTF_8))) {
            return GoogleCredential.fromStream(inputStream)
                    .createScoped(SCOPES);
        }
    }
} 