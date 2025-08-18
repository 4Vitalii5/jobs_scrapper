package com.example.techstars.service.impl;

import com.example.techstars.config.GoogleSheetsProperties;
import com.example.techstars.dto.ColumnDefinition;
import com.example.techstars.dto.JobDto;
import com.example.techstars.mapper.JobMapper;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.Exporter;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetExportServiceImpl implements Exporter {

    private static final String FORMAT = "sheets";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    private static final List<ColumnDefinition> COLUMN_DEFINITIONS = List.of(
            ColumnDefinition.builder().header("Position Name").extractor(JobDto::getPositionName)
                    .build(),
            ColumnDefinition.builder().header("Job Page URL").extractor(JobDto::getJobPageUrl)
                    .build(),
            ColumnDefinition.builder().header("Labor Function").extractor(JobDto::getLaborFunction)
                    .build(),
            ColumnDefinition.builder().header("Posted Date").extractor(job -> formatDate(job.getPostedDate()))
                    .build(),
            ColumnDefinition.builder().header("Locations").extractor(job -> String.join(", ", job.getLocations()))
                    .build(),
            ColumnDefinition.builder().header("Organization Title").extractor(job -> job.getOrganization().getTitle())
                    .build(),
            ColumnDefinition.builder().header("Organization URL").extractor(job -> job.getOrganization().getUrl())
                    .build(),
            ColumnDefinition.builder().header("Organization Logo").extractor(job -> job.getOrganization().getLogoUrl())
                    .build(),
            ColumnDefinition.builder().header("Tags").extractor(job -> String.join(", ", job.getTags()))
                    .build(),
            ColumnDefinition.builder().header("Description").extractor(JobDto::getDescription)
                    .build()
    );

    private final JobMapper jobMapper;
    private final GoogleSheetsProperties sheetsProperties;
    private final JobRepository jobRepository;

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public String exportByFunction(String laborFunction) {
        try {
            List<JobDto> jobDtos = fetchAndMapJobs(laborFunction);
            if (jobDtos.isEmpty()) {
                return "No jobs found for function: " + laborFunction;
            }

            Sheets service = buildSheetsService();

            ensureSheetExists(service, laborFunction);

            uploadDataToSheet(service, jobDtos, laborFunction);

            return "Successfully exported " + jobDtos.size() + " jobs for '" + laborFunction
                    + "' to Google Sheets.";
        } catch (Exception e) {
            log.error("Error exporting jobs to Google Sheets: {}", e.getMessage(), e);
            return "An error occurred during export: " + e.getMessage();
        }
    }

    private List<JobDto> fetchAndMapJobs(String laborFunction) {
        List<Job> jobs = jobRepository.findByLaborFunction(laborFunction);
        return jobs.stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    private Sheets buildSheetsService() throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials = getCredentialsFromBase64();

        return new Sheets.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Techstars Job Scraper")
                .build();
    }

    private void uploadDataToSheet(Sheets service, List<JobDto> jobs, String sheetName) throws IOException {
        String spreadsheetId = sheetsProperties.getSpreadsheetId();
        String formattedSheetName = formatSheetName(sheetName);

        String clearRange = formattedSheetName + "!A:Z";
        service.spreadsheets().values().clear(spreadsheetId, clearRange, new ClearValuesRequest()).execute();

        List<List<Object>> data = prepareDataForUpload(jobs);
        ValueRange body = new ValueRange().setValues(data);

        String updateRange = formattedSheetName + "!A1";
        service.spreadsheets().values().update(spreadsheetId, updateRange, body)
                .setValueInputOption("RAW")
                .execute();
    }

    private String formatSheetName(String sheetName) {
        if (sheetName.contains(" ") || sheetName.contains("-")) {
            return "'" + sheetName + "'";
        }
        return sheetName;
    }

    private void ensureSheetExists(Sheets service, String sheetName) throws IOException {
        String spreadsheetId = sheetsProperties.getSpreadsheetId();

        if (StringUtils.isBlank(spreadsheetId)) {
            throw new IllegalStateException("Google Sheets spreadsheetId is not configured.");
        }

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

    private GoogleCredentials getCredentialsFromBase64() throws IOException {
        String credentialsBase64 = sheetsProperties.getCredentials().getBase64();

        if (StringUtils.isBlank(credentialsBase64)) {
            throw new IllegalStateException("Google Sheets credentials not configured. " +
                    "Please set the GOOGLE_CREDENTIALS_JSON environment variable.");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            return ServiceAccountCredentials.fromStream(inputStream)
                    .createScoped(SCOPES);
        }
    }
}