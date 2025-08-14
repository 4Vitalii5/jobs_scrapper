package com.example.techstars.controller;

import com.example.techstars.dto.JobDto;
import com.example.techstars.service.ExportService;
import com.example.techstars.service.JobService;
import com.example.techstars.service.SheetExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Job Controller", description = "Endpoints for retrieving and filtering jobs")
public class JobController {
    private final JobService jobService;
    private final ExportService databaseExportService;
    private final SheetExportService googleSheetsService;

    @GetMapping
    @Operation(summary = "Get all jobs", description = "Returns a paginated list of all jobs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")
    })
    public ResponseEntity<Page<JobDto>> getAllJobs(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job found"),
            @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    })
    public ResponseEntity<JobDto> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/function/{laborFunction}")
    @Operation(summary = "Get jobs by labor function")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<List<JobDto>> getJobsByFunction(
            @Parameter(description = "Labor function to filter by (e.g., 'Software Engineering')")
            @PathVariable String laborFunction
    ) {
        return ResponseEntity.ok(jobService.getJobsByFunction(laborFunction));
    }

    @GetMapping("/location/{location}")
    @Operation(summary = "Get jobs by location")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<List<JobDto>> getJobsByLocation(@PathVariable String location) {
        return ResponseEntity.ok(jobService.getJobsByLocation(location));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get jobs by date range")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<List<JobDto>> getJobsByDateRange(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        return ResponseEntity.ok(jobService.getJobsByDateRange(startDate, endDate));
    }

    @GetMapping("/functions")
    @Operation(summary = "Get all labor functions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<List<String>> getAllLaborFunctions() {
        return ResponseEntity.ok(jobService.getAllLaborFunctions());
    }

    @GetMapping("/locations")
    @Operation(summary = "Get all locations")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<List<String>> getAllLocations() {
        return ResponseEntity.ok(jobService.getAllLocations());
    }

    @GetMapping("/count/{function}")
    @Operation(summary = "Get job quantity by labor function")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<Long> getJobCountByFunction(@PathVariable String function) {
        return ResponseEntity.ok(jobService.getJobCountByFunction(function));
    }

    @PostMapping("/export/sql/{laborFunction}")
    @Operation(summary = "Export jobs to sql by labor function")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<String> exportJobsToSql(@PathVariable String laborFunction) throws IOException {
        String fileName = databaseExportService.exportJobsByFunctionToSql(laborFunction);
        return ResponseEntity.ok("Jobs exported to SQL file: " + fileName);
    }

    @PostMapping("/export/sheets/{laborFunction}")
    @Operation(summary = "Export jobs to google sheets")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved jobs")})
    public ResponseEntity<String> exportJobsToGoogleSheets(@PathVariable String laborFunction) {
        return googleSheetsService.exportJobsToGoogleSheets(laborFunction)
                ? ResponseEntity.ok("Jobs exported to Google Sheets successfully")
                : ResponseEntity.internalServerError().body("Failed to export jobs to Google Sheets");
    }
} 