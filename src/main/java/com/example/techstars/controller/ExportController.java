package com.example.techstars.controller;

import com.example.techstars.service.Exporter;
import com.example.techstars.service.impl.ExportServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/export")
@CrossOrigin(origins = "*")
@Tag(name = "Export Controller", description = "Endpoints for exporting job data in various formats")
public class ExportController {

    private final Map<String, Exporter> exporters;
    private final ExportServiceImpl sqlExportService;

    @Autowired
    public ExportController(List<Exporter> exporterList, ExportServiceImpl sqlExportService) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(Exporter::getFormat, Function.identity()));
        this.sqlExportService = sqlExportService;
    }

    @PostMapping("/{format}/{laborFunction}")
    @Operation(summary = "Export jobs by function to a specified format (e.g., sql, sheets)")
    public ResponseEntity<String> exportJobsByFunction(
            @Parameter(description = "The format to export to (e.g., 'sql', 'sheets')") @PathVariable String format,
            @Parameter(description = "Labor function to filter by") @PathVariable String laborFunction) throws IOException {

        Exporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported export format: " + format);
        }

        String result = exporter.exportByFunction(laborFunction);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sql/database")
    @Operation(summary = "Export full database to a SQL file")
    public ResponseEntity<String> exportFullDatabase() throws IOException {
        String result = sqlExportService.exportDatabaseToSqlFile();
        return ResponseEntity.ok(result);
    }
}