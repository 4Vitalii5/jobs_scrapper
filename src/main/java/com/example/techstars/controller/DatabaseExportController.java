package com.example.techstars.controller;

import com.example.techstars.service.DatabaseExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatabaseExportController {

    private final DatabaseExportService databaseExportService;

    @PostMapping("/database")
    public ResponseEntity<String> exportFullDatabase() {
        try {
            String fileName = databaseExportService.exportDatabaseToSqlFile();
            return ResponseEntity.ok("Database exported successfully to: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error exporting database: " + e.getMessage());
        }
    }

    @PostMapping("/jobs/{laborFunction}")
    public ResponseEntity<String> exportJobsByFunction(@PathVariable String laborFunction) {
        try {
            String fileName = databaseExportService.exportJobsByFunctionToSql(laborFunction);
            return ResponseEntity.ok("Jobs exported successfully to: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error exporting jobs: " + e.getMessage());
        }
    }
} 