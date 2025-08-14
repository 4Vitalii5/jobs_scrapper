package com.example.techstars.controller;

import com.example.techstars.dto.JobDTO;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.DatabaseExportService;
import com.example.techstars.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobRepository jobRepository;
    private final DatabaseExportService databaseExportService;
    private final GoogleSheetsService googleSheetsService;

    @GetMapping
    public ResponseEntity<Page<JobDTO>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Job> jobsPage = jobRepository.findAll(pageable);
        Page<JobDTO> jobsDTOPage = jobsPage.map(JobDTO::fromEntity);
        
        return ResponseEntity.ok(jobsDTOPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(JobDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/function/{laborFunction}")
    public ResponseEntity<List<JobDTO>> getJobsByFunction(@PathVariable String laborFunction) {
        List<Job> jobs = jobRepository.findByLaborFunction(laborFunction);
        List<JobDTO> jobDTOs = jobs.stream()
                .map(JobDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<JobDTO>> getJobsByLocation(@PathVariable String location) {
        List<Job> jobs = jobRepository.findByLocationContainingIgnoreCase(location);
        List<JobDTO> jobDTOs = jobs.stream()
                .map(JobDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<JobDTO>> getJobsByDateRange(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        List<Job> jobs = jobRepository.findByPostedDateBetween(startDate, endDate);
        List<JobDTO> jobDTOs = jobs.stream()
                .map(JobDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/functions")
    public ResponseEntity<List<String>> getAllLaborFunctions() {
        List<String> functions = jobRepository.findAllLaborFunctions();
        return ResponseEntity.ok(functions);
    }

    @GetMapping("/locations")
    public ResponseEntity<List<String>> getAllLocations() {
        List<String> locations = jobRepository.findAllLocations();
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/count/{function}")
    public ResponseEntity<Long> getJobCountByFunction(@PathVariable String function) {
        long count = jobRepository.countByLaborFunction(function);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/export/sql/{laborFunction}")
    public ResponseEntity<String> exportJobsToSql(@PathVariable String laborFunction) {
        try {
            String fileName = databaseExportService.exportJobsByFunctionToSql(laborFunction);
            return ResponseEntity.ok("Jobs exported to SQL file: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error exporting jobs: " + e.getMessage());
        }
    }

    @PostMapping("/export/sheets/{laborFunction}")
    public ResponseEntity<String> exportJobsToGoogleSheets(@PathVariable String laborFunction) {
        try {
            boolean success = googleSheetsService.exportJobsToGoogleSheets(laborFunction);
            if (success) {
                return ResponseEntity.ok("Jobs exported to Google Sheets successfully");
            } else {
                return ResponseEntity.internalServerError()
                        .body("Failed to export jobs to Google Sheets");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error exporting jobs to Google Sheets: " + e.getMessage());
        }
    }
} 