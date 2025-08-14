package com.example.techstars.service;

import com.example.techstars.model.Job;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.OrganizationRepository;
import com.example.techstars.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseExportService {

    @Value("${app.db.export.pg_dump_path:pg_dump}")
    private String pgDumpPath;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final TagRepository tagRepository;

    public String exportDatabaseToSqlFile() {
        try {
            String fileName = "techstars_jobs_export_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
            Path filePath = Paths.get(fileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Write database creation schema
                writeDatabaseSchema(writer);
                
                // Write data
                writeData(writer);
            }

            log.info("Database exported successfully to: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Error exporting database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export database", e);
        }
    }

    private void writeDatabaseSchema(FileWriter writer) throws IOException {
        writer.write("-- Techstars Jobs Database Export\n");
        writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");
        
        writer.write("-- Create database\n");
        writer.write("CREATE DATABASE techstars_db;\n");
        writer.write("\\c techstars_db;\n\n");
        
        writer.write("-- Create tables\n");
        writer.write("CREATE TABLE organization (\n");
        writer.write("    id BIGSERIAL PRIMARY KEY,\n");
        writer.write("    title VARCHAR(255) NOT NULL,\n");
        writer.write("    url VARCHAR(500) NOT NULL\n");
        writer.write(");\n\n");
        
        writer.write("CREATE TABLE tag (\n");
        writer.write("    id BIGSERIAL PRIMARY KEY,\n");
        writer.write("    name VARCHAR(255) NOT NULL UNIQUE\n");
        writer.write(");\n\n");
        
        writer.write("CREATE TABLE job (\n");
        writer.write("    id BIGSERIAL PRIMARY KEY,\n");
        writer.write("    position_name VARCHAR(500) NOT NULL,\n");
        writer.write("    job_page_url VARCHAR(500) NOT NULL UNIQUE,\n");
        writer.write("    logo_url VARCHAR(500) NOT NULL,\n");
        writer.write("    labor_function VARCHAR(255) NOT NULL,\n");
        writer.write("    posted_date BIGINT NOT NULL,\n");
        writer.write("    description TEXT,\n");
        writer.write("    location VARCHAR(255) NOT NULL,\n");
        writer.write("    address VARCHAR(255) NOT NULL,\n");
        writer.write("    organization_id BIGINT REFERENCES organization(id)\n");
        writer.write(");\n\n");
        
        writer.write("CREATE TABLE job_tag (\n");
        writer.write("    job_id BIGINT REFERENCES job(id),\n");
        writer.write("    tag_id BIGINT REFERENCES tag(id),\n");
        writer.write("    PRIMARY KEY (job_id, tag_id)\n");
        writer.write(");\n\n");
        
        writer.write("-- Create indexes\n");
        writer.write("CREATE INDEX idx_job_labor_function ON job(labor_function);\n");
        writer.write("CREATE INDEX idx_job_location ON job(location);\n");
        writer.write("CREATE INDEX idx_job_posted_date ON job(posted_date);\n");
        writer.write("CREATE INDEX idx_organization_url ON organization(url);\n");
        writer.write("CREATE INDEX idx_tag_name ON tag(name);\n\n");
    }

    private void writeData(FileWriter writer) throws IOException {
        // Write organizations
        List<Organization> organizations = organizationRepository.findAll();
        if (!organizations.isEmpty()) {
            writer.write("-- Insert organizations\n");
            for (Organization org : organizations) {
                writer.write(String.format("INSERT INTO organization (id, title, url) VALUES (%d, '%s', '%s');\n",
                        org.getId(), escapeSql(org.getTitle()), escapeSql(org.getUrl())));
            }
            writer.write("\n");
        }

        // Write tags
        List<Tag> tags = tagRepository.findAll();
        if (!tags.isEmpty()) {
            writer.write("-- Insert tags\n");
            for (Tag tag : tags) {
                writer.write(String.format("INSERT INTO tag (id, name) VALUES (%d, '%s');\n",
                        tag.getId(), escapeSql(tag.getName())));
            }
            writer.write("\n");
        }

        // Write jobs
        List<Job> jobs = jobRepository.findAll();
        if (!jobs.isEmpty()) {
            writer.write("-- Insert jobs\n");
            for (Job job : jobs) {
                writer.write(String.format("INSERT INTO job (id, position_name, job_page_url, logo_url, labor_function, posted_date, description, location, address, organization_id) VALUES (%d, '%s', '%s', '%s', '%s', %d, '%s', '%s', '%s', %d);\n",
                        job.getId(),
                        escapeSql(job.getPositionName()),
                        escapeSql(job.getJobPageUrl()),
                        escapeSql(job.getLogoUrl()),
                        escapeSql(job.getLaborFunction()),
                        job.getPostedDate(),
                        escapeSql(job.getDescription()),
                        escapeSql(job.getLocation()),
                        escapeSql(job.getAddress()),
                        job.getOrganization().getId()));
            }
            writer.write("\n");
        }

        // Write job-tag relationships
        if (!jobs.isEmpty()) {
            writer.write("-- Insert job-tag relationships\n");
            for (Job job : jobs) {
                for (Tag tag : job.getTags()) {
                    writer.write(String.format("INSERT INTO job_tag (job_id, tag_id) VALUES (%d, %d);\n",
                            job.getId(), tag.getId()));
                }
            }
            writer.write("\n");
        }

        // Reset sequences
        writer.write("-- Reset sequences\n");
        writer.write("SELECT setval('organization_id_seq', (SELECT MAX(id) FROM organization));\n");
        writer.write("SELECT setval('tag_id_seq', (SELECT MAX(id) FROM tag));\n");
        writer.write("SELECT setval('job_id_seq', (SELECT MAX(id) FROM job));\n");
    }

    private String escapeSql(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "''").replace("\\", "\\\\");
    }

    public String exportJobsByFunctionToSql(String laborFunction) {
        try {
            String fileName = "techstars_" + laborFunction.replaceAll("[^a-zA-Z0-9]", "_") + "_export_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
            Path filePath = Paths.get(fileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("-- Techstars Jobs Export for Function: " + laborFunction + "\n");
                writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");
                
                writeDatabaseSchema(writer);
                
                // Write only jobs for specific function
                List<Job> jobs = jobRepository.findByLaborFunction(laborFunction);
                if (!jobs.isEmpty()) {
                    writer.write("-- Insert organizations\n");
                    jobs.stream()
                            .map(Job::getOrganization)
                            .distinct()
                            .forEach(org -> {
                                try {
                                    writer.write(String.format("INSERT INTO organization (id, title, url) VALUES (%d, '%s', '%s');\n",
                                            org.getId(), escapeSql(org.getTitle()), escapeSql(org.getUrl())));
                                } catch (IOException e) {
                                    log.error("Error writing organization: {}", e.getMessage());
                                }
                            });
                    writer.write("\n");

                    writer.write("-- Insert tags\n");
                    jobs.stream()
                            .flatMap(job -> job.getTags().stream())
                            .distinct()
                            .forEach(tag -> {
                                try {
                                    writer.write(String.format("INSERT INTO tag (id, name) VALUES (%d, '%s');\n",
                                            tag.getId(), escapeSql(tag.getName())));
                                } catch (IOException e) {
                                    log.error("Error writing tag: {}", e.getMessage());
                                }
                            });
                    writer.write("\n");

                    writer.write("-- Insert jobs\n");
                    for (Job job : jobs) {
                        writer.write(String.format("INSERT INTO job (id, position_name, job_page_url, logo_url, labor_function, posted_date, description, location, address, organization_id) VALUES (%d, '%s', '%s', '%s', '%s', %d, '%s', '%s', '%s', %d);\n",
                                job.getId(),
                                escapeSql(job.getPositionName()),
                                escapeSql(job.getJobPageUrl()),
                                escapeSql(job.getLogoUrl()),
                                escapeSql(job.getLaborFunction()),
                                job.getPostedDate(),
                                escapeSql(job.getDescription()),
                                escapeSql(job.getLocation()),
                                escapeSql(job.getAddress()),
                                job.getOrganization().getId()));
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
                }

                // Reset sequences
                writer.write("-- Reset sequences\n");
                writer.write("SELECT setval('organization_id_seq', (SELECT MAX(id) FROM organization));\n");
                writer.write("SELECT setval('tag_id_seq', (SELECT MAX(id) FROM tag));\n");
                writer.write("SELECT setval('job_id_seq', (SELECT MAX(id) FROM job));\n");
            }

            log.info("Jobs for function '{}' exported successfully to: {}", laborFunction, fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Error exporting jobs for function '{}': {}", laborFunction, e.getMessage(), e);
            throw new RuntimeException("Failed to export jobs for function: " + laborFunction, e);
        }
    }
} 