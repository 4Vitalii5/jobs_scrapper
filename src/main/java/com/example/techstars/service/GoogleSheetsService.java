package com.example.techstars.service;

import com.example.techstars.dto.JobDTO;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    @Value("${google.sheets.credentials.path:}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet.id:}")
    private String spreadsheetId;

    private final JobRepository jobRepository;

    public boolean exportJobsToGoogleSheets(String laborFunction) {
        try {
            List<Job> jobs = jobRepository.findByLaborFunction(laborFunction);
            if (jobs.isEmpty()) {
                log.warn("No jobs found for function: {}", laborFunction);
                return false;
            }

            List<JobDTO> jobDTOs = jobs.stream()
                    .map(JobDTO::fromEntity)
                    .collect(Collectors.toList());

            return uploadToGoogleSheets(jobDTOs, laborFunction);
        } catch (Exception e) {
            log.error("Error exporting jobs to Google Sheets: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean uploadToGoogleSheets(List<JobDTO> jobs, String sheetName) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(httpTransport);

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("Techstars Job Scraper")
                    .build();

            // Prepare data for upload
            List<List<Object>> data = prepareDataForUpload(jobs);

            // Create or update sheet
            ValueRange body = new ValueRange().setValues(data);
            
            // Clear existing data and upload new data
            String range = sheetName + "!A1";
            service.spreadsheets().values()
                    .clear(spreadsheetId, sheetName + "!A:Z", new ClearValuesRequest())
                    .execute();
            
            service.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();

            log.info("Successfully uploaded {} jobs to Google Sheets", jobs.size());
            return true;
        } catch (Exception e) {
            log.error("Error uploading to Google Sheets: {}", e.getMessage(), e);
            return false;
        }
    }

    private List<List<Object>> prepareDataForUpload(List<JobDTO> jobs) {
        List<List<Object>> data = new ArrayList<>();
        
        // Add header row
        List<Object> header = List.of(
                "Position Name", "Job Page URL", "Logo URL", "Labor Function",
                "Posted Date", "Location", "Address", "Organization Title",
                "Organization URL", "Tags", "Description"
        );
        data.add(header);

        // Add data rows
        for (JobDTO job : jobs) {
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
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            throw new IllegalStateException("Google Sheets credentials path not configured");
        }

        try (FileInputStream inputStream = new FileInputStream(credentialsPath)) {
            return GoogleCredential.fromStream(inputStream)
                    .createScoped(SCOPES);
        }
    }
} 