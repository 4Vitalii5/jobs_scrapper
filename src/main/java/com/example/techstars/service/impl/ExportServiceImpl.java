package com.example.techstars.service.impl;

import com.example.techstars.model.Job;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.ExportService;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {
    private final JobRepository jobRepository;

    @Override
    public String exportDatabaseToSqlFile() throws IOException {
        String fileName = "techstars_jobs_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        Path filePath = Paths.get(fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("-- Techstars Jobs Full Data Export\n");
            writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");

            List<Job> allJobs = jobRepository.findAll();
            writeDataForJobs(writer, allJobs);
        }

        log.info("Database data exported successfully to: {}", fileName);
        return fileName;
    }

    @Override
    public String exportJobsByFunctionToSql(String laborFunction) throws IOException {
        String fileName =
                "techstars_" + laborFunction.replaceAll("[^a-zA-Z0-9]", "_") + "_export_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        + ".sql";
        Path filePath = Paths.get(fileName);

        List<Job> jobsByFunction = jobRepository.findByLaborFunction(laborFunction);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("-- Techstars Jobs Export for Function: " + laborFunction + "\n");
            writer.write("-- Generated on: " + LocalDateTime.now() + "\n\n");

            writeDataForJobs(writer, jobsByFunction);
        }

        log.info("Jobs for function '{}' exported successfully to: {}", laborFunction, fileName);
        return fileName;
    }

    private void writeDataForJobs(FileWriter writer, List<Job> jobs) throws IOException {
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

        if (!organizations.isEmpty()) {
            writer.write("-- Insert organizations\n");
            for (Organization org : organizations) {
                writer.write(String.format("INSERT INTO organization (id, title, url) VALUES (%d, '%s', '%s');\n",
                        org.getId(), escapeSql(org.getTitle()), escapeSql(org.getUrl())));
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
                    "INSERT INTO job (id, position_name, job_page_url, logo_url, "
                            + "labor_function, posted_date, description, location, address, organization_id) "
                            + "VALUES (%d, '%s', '%s', '%s', '%s', %d, '%s', '%s', '%s', %d);\n",
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

        writer.write("-- Reset sequences to avoid conflicts on next inserts\n");
        writer.write("SELECT setval('organization_id_seq', (SELECT MAX(id) FROM organization), true);\n");
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