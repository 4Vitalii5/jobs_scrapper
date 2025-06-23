package com.example.techstars.controller;

import com.example.techstars.service.DatabaseExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DatabaseExportController {
    private final DatabaseExportService databaseExportService;

    @PostMapping("/export-sql")
    public String exportSql(@RequestParam(defaultValue = "./techstars_dump.sql") String filePath) {
        return databaseExportService.exportDatabaseToSqlFile(filePath);
    }
} 