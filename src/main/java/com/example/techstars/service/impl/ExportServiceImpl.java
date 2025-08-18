package com.example.techstars.service.impl;

import com.example.techstars.model.Job;
import com.example.techstars.model.Location;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.Exporter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements Exporter {
    private final JobRepository jobRepository;
    private static final String FORMAT = "sql";

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportByFunction(String laborFunction) throws IOException {
        String fileName =
                "techstars_" + laborFunction.replaceAll("[^a-zA-Z0-9]", "_") + "_export_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        + ".sql";
        Path filePath = Paths.get(fileName);

        List<Job> jobsByFunction = jobRepository.findByLaborFunction(laborFunction);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("-- Techstars Jobs Export for Function: " + laborFunction + "\n");
            writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");

            writeSchemaCreationScript(writer);

            writeDataForJobs(writer, jobsByFunction);
        }

        return "Jobs for function '" + laborFunction + "' exported successfully to file: "
                + fileName;
    }

    @Transactional(readOnly = true)
    public String exportDatabaseToSqlFile() throws IOException {
        String fileName = "techstars_jobs_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        Path filePath = Paths.get(fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("-- Techstars Jobs Full Data Export\n");
            writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");

            writeSchemaCreationScript(writer);

            List<Job> allJobs = jobRepository.findAll();
            writeDataForJobs(writer, allJobs);
        }

        return "Full database exported successfully to file: " + fileName;
    }

    private void writeSchemaCreationScript(FileWriter writer) throws IOException {
        log.info("Writing database schema to the export file...");
        writer.write("-- ======================================================================\n");
        writer.write("-- Database Schema Creation\n");
        writer.write("-- ======================================================================\n\n");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] migrationFiles = resolver.getResources("classpath:db/migration/*.sql");

        Arrays.sort(migrationFiles, Comparator.comparing(Resource::getFilename));

        for (Resource resource : migrationFiles) {
            writer.write("-- Sourcing migration: " + resource.getFilename() + "\n");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bdata = FileCopyUtils.copyToByteArray(inputStream);
                String data = new String(bdata, StandardCharsets.UTF_8);
                writer.write(data);
                writer.write("\n\n");
            }
        }
        log.info("Successfully wrote {} migration scripts.", migrationFiles.length);
    }

    private void writeDataForJobs(FileWriter writer, List<Job> jobs) throws IOException {
        writer.write("-- ======================================================================\n");
        writer.write("-- Data Insertion\n");
        writer.write("-- ======================================================================\n\n");

        if (jobs.isEmpty()) {
            writer.write("-- No jobs found to export.\n");
            return;
        }

        List<Organization> organizations = jobs.stream()
                .map(Job::getOrganization)
                .distinct()
                .toList();

        List<Tag> tags = jobs.stream()
                .flatMap(job -> job.getTags().stream())
                .distinct()
                .toList();

        List<Location> locations = jobs.stream()
                .flatMap(job -> job.getLocations().stream())
                .distinct()
                .toList();


        if (!organizations.isEmpty()) {
            writer.write("-- Insert organizations\n");
            for (Organization org : organizations) {
                writer.write(String.format("INSERT INTO organization (id, title, url, logo_url) VALUES (%d, '%s', '%s', '%s');\n",
                        org.getId(), escapeSql(org.getTitle()), escapeSql(org.getUrl()), escapeSql(org.getLogoUrl())));
            }
            writer.write("\n");
        }

        if (!locations.isEmpty()) {
            writer.write("-- Insert locations\n");
            for (Location location : locations) {
                writer.write(String.format("INSERT INTO location (id, name) VALUES (%d, '%s');\n",
                        location.getId(), escapeSql(location.getName())));
            }
            writer.write("\n");
        }

        if (!tags.isEmpty()) {
            writer.write("-- Insert tags\n");
            for (Tag tag : tags) {
                writer.write(String.format("INSERT INTO tag (id, name) VALUES (%d, '%s');\n",
                        tag.getId(), escapeSql(tag.getName())));
            }
            writer.write("\n");
        }


        writer.write("-- Insert jobs\n");
        for (Job job : jobs) {
            writer.write(String.format(
                    "INSERT INTO job (id, position_name, job_page_url, "
                            + "labor_function, posted_date, description, organization_id) "
                            + "VALUES (%d, '%s', '%s', '%s', %d, '%s', %d);\n",
                    job.getId(),
                    escapeSql(job.getPositionName()),
                    escapeSql(job.getJobPageUrl()),
                    escapeSql(job.getLaborFunction()),
                    job.getPostedDate(),
                    escapeSql(job.getDescription()),
                    job.getOrganization().getId()));
        }
        writer.write("\n");

        writer.write("-- Insert job-location relationships\n");
        for (Job job : jobs) {
            for (Location location : job.getLocations()) {
                writer.write(String.format("INSERT INTO job_location (job_id, location_id) VALUES (%d, %d);\n",
                        job.getId(), location.getId()));
            }
        }
        writer.write("\n");

        writer.write("-- Insert job-tag relationships\n");
        for (Job job : jobs) {
            for (Tag tag : job.getTags()) {
                writer.write(String.format("INSERT INTO job_tag (job_id, tag_id) VALUES (%d, %d);\n",
                        job.getId(), tag.getId()));
            }
        }
        writer.write("\n");

        writer.write("-- Reset sequences to avoid conflicts on next inserts\n");
        writer.write("SELECT setval('organization_id_seq', (SELECT MAX(id) FROM organization), true);\n");
        writer.write("SELECT setval('location_id_seq', (SELECT MAX(id) FROM location), true);\n");
        writer.write("SELECT setval('tag_id_seq', (SELECT MAX(id) FROM tag), true);\n");
        writer.write("SELECT setval('job_id_seq', (SELECT MAX(id) FROM job), true);\n");
    }

    private String escapeSql(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "''");
    }
}