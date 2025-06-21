package com.example.techstars.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DatabaseExportService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseExportService.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.db.export.pg_dump_path:pg_dump}")
    private String pgDumpPath;

    /**
     * Exports the PostgreSQL database to a .sql file using pg_dump.
     * Requires pg_dump to be installed and accessible in PATH.
     *
     * @param filePath The path to the output .sql file
     * @return The file path if successful, or an error message
     */
    public String exportDatabaseToSqlFile(String filePath) {
        String dbName = extractDbNameFromUrl(dbUrl);
        String dbHost = extractHostFromUrl(dbUrl);
        String dbPort = extractPortFromUrl(dbUrl);

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpPath,
                "-h", dbHost,
                "-p", dbPort,
                "-U", dbUser,
                "-F", "p",
                "-f", filePath,
                dbName
        );

        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        try {
            log.info("Executing pg_dump: {}", String.join(" ", pb.command()));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                String successMessage = "Database exported successfully to: " + filePath;
                log.info(successMessage);
                return successMessage;
            } else {
                String errorMessage = "pg_dump failed with exit code: " + exitCode;
                log.error(errorMessage);
                return errorMessage;
            }
        } catch (IOException e) {
            String errorMessage = "Error during export: " + e.getMessage();
            log.error(errorMessage, e);
            
            // Provide helpful guidance for common issues
            if (e.getMessage().contains("Cannot run program") || e.getMessage().contains("CreateProcess error=2")) {
                errorMessage += "\n\nTroubleshooting:\n" +
                    "1. Make sure PostgreSQL is installed and pg_dump is in your PATH\n" +
                    "2. Try setting the full path to pg_dump in application.properties:\n" +
                    "   app.db.export.pg_dump_path=C:/Program Files/PostgreSQL/[version]/bin/pg_dump.exe\n" +
                    "3. Or add PostgreSQL bin directory to your system PATH\n" +
                    "4. Common PostgreSQL installation paths:\n" +
                    "   - Windows: C:/Program Files/PostgreSQL/[version]/bin/\n" +
                    "   - Linux/Mac: /usr/bin/pg_dump or /usr/local/bin/pg_dump";
            }
            
            return errorMessage;
        } catch (InterruptedException e) {
            log.error("Error during database export", e);
            Thread.currentThread().interrupt();
            return "Error during export: " + e.getMessage();
        }
    }

    private String extractDbNameFromUrl(String url) {
        try {
            return url.substring(url.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private String extractHostFromUrl(String url) {
        try {
            String temp = url.split("//")[1];
            return temp.split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String extractPortFromUrl(String url) {
        try {
            String temp = url.split("//")[1];
            String[] parts = temp.split(":");
            return parts[1].split("/")[0];
        } catch (Exception e) {
            return "5432"; // Default PostgreSQL port
        }
    }
} 