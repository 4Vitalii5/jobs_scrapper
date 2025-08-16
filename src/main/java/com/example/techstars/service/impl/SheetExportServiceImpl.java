package com.example.techstars.service.impl;

import com.example.techstars.config.GoogleSheetsProperties;
import com.example.techstars.dto.JobDto;
import com.example.techstars.mapper.JobMapper;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.SheetExportService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
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

    private record ColumnDefinition(String header, Function<JobDto, Object> extractor) {
    }

    private static final List<ColumnDefinition> COLUMN_DEFINITIONS = List.of(
            new ColumnDefinition("Position Name", JobDto::getPositionName),
            new ColumnDefinition("Job Page URL", JobDto::getJobPageUrl),
            new ColumnDefinition("Labor Function", JobDto::getLaborFunction),
            new ColumnDefinition("Posted Date", job -> formatDate(job.getPostedDate())),
            new ColumnDefinition("Locations", job -> String.join(", ", job.getLocations())),
            new ColumnDefinition("Organization Title", job -> job.getOrganization().getTitle()),
            new ColumnDefinition("Organization URL", job -> job.getOrganization().getUrl()),
            new ColumnDefinition("Organization Logo", job -> job.getOrganization().getLogoUrl()),
            new ColumnDefinition("Tags", job -> String.join(", ", job.getTags())),
            new ColumnDefinition("Description", JobDto::getDescription)
    );

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
            HttpCredentialsAdapter credential = getCredentials(httpTransport);

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("Techstars Job Scraper")
                    .build();

            ensureSheetExists(service, sheetsProperties.getSpreadsheetId(), sheetName);

            String formattedSheetName = formatSheetName(sheetName);

            String clearRange = formattedSheetName + "!A:Z";
            service.spreadsheets().values()
                    .clear(sheetsProperties.getSpreadsheetId(), clearRange, new ClearValuesRequest())
                    .execute();

            List<List<Object>> data = prepareDataForUpload(jobs);
            ValueRange body = new ValueRange().setValues(data);

            String updateRange = formattedSheetName + "!A1";
            service.spreadsheets().values()
                    .update(sheetsProperties.getSpreadsheetId(), updateRange, body)
                    .setValueInputOption("RAW")
                    .execute();

            log.info("Successfully uploaded {} jobs to Google Sheets sheet '{}'", jobs.size(), sheetName);
            return true;
        } catch (Exception e) {
            log.error("Error uploading to Google Sheets: {}", e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
    }

    private String formatSheetName(String sheetName) {
        if (sheetName.contains(" ") || sheetName.contains("-")) {
            return "'" + sheetName + "'";
        }
        return sheetName;
    }

    private void ensureSheetExists(Sheets service, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        boolean sheetExists = spreadsheet.getSheets().stream()
                .anyMatch(sheet -> sheet.getProperties().getTitle().equalsIgnoreCase(sheetName));

        if (!sheetExists) {
            log.info("Sheet '{}' not found, creating it.", sheetName);
            SheetProperties properties = new SheetProperties().setTitle(sheetName);
            AddSheetRequest addSheetRequest = new AddSheetRequest().setProperties(properties);
            Request request = new Request().setAddSheet(addSheetRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(List.of(request));
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Sheet '{}' created successfully.", sheetName);
        }
    }

    private List<List<Object>> prepareDataForUpload(List<JobDto> jobs) {
        List<List<Object>> data = new ArrayList<>();

        List<Object> header = COLUMN_DEFINITIONS.stream()
                .map(ColumnDefinition::header)
                .collect(Collectors.toList());
        data.add(header);

        for (JobDto job : jobs) {
            List<Object> row = COLUMN_DEFINITIONS.stream()
                    .map(ColumnDefinition::extractor)
                    .map(extractor -> extractor.apply(job))
                    .collect(Collectors.toList());
            data.add(row);
        }

        return data;
    }

    private static String formatDate(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "N/A";
        }
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private HttpCredentialsAdapter getCredentials(NetHttpTransport httpTransport) throws IOException {
        String credentialsBase64 = sheetsProperties.getCredentials().getBase64();

        if (StringUtils.isBlank(credentialsBase64)) {
            throw new IllegalStateException("Google Sheets credentials not configured. Please set GOOGLE_CREDENTIALS_BASE64 environment variable.");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream)
                    .createScoped(SCOPES);

            return new HttpCredentialsAdapter(credentials);
        }
    }
}