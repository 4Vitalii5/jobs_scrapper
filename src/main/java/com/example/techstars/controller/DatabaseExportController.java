package com.example.techstars.controller;

import com.example.techstars.service.ExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Export Controller", description = "Endpoints for exporting job data")
public class DatabaseExportController {

    private final ExportService exportService;

    @PostMapping("/database")
    public ResponseEntity<String> exportFullDatabase() throws IOException {
        String fileName = exportService.exportDatabaseToSqlFile();
        return ResponseEntity.ok("Database exported successfully to: " + fileName);
    }

    @PostMapping("/jobs/{laborFunction}")
    public ResponseEntity<String> exportJobsByFunction(@PathVariable String laborFunction) throws IOException {
        String fileName = exportService.exportJobsByFunctionToSql(laborFunction);
        return ResponseEntity.ok("Jobs exported successfully to: " + fileName);
    }
} 