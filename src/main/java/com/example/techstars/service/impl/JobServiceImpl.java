package com.example.techstars.service.impl;

import com.example.techstars.dto.JobDto;
import com.example.techstars.dto.PageDto;
import com.example.techstars.exception.ResourceNotFoundException;
import com.example.techstars.mapper.JobMapper;
import com.example.techstars.model.Job;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.service.JobService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Override
    public PageDto<JobDto> getAllJobs(Pageable pageable) {
        Page<Job> jobPage = jobRepository.findAll(pageable);

        List<JobDto> jobDtos = jobPage.getContent().stream()
                .map(jobMapper::toDto)
                .toList();
        return new PageDto<>(jobDtos, jobPage);
    }

    @Override
    public JobDto getJobById(Long id) {
        return jobRepository.findById(id)
                .map(jobMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job with ID " + id + " not found."));
    }

    @Override
    public List<JobDto> getJobsByFunction(String laborFunction) {
        return jobRepository.findByLaborFunction(laborFunction).stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getJobsByLocation(String location) {
        return jobRepository.findByLocationContainingIgnoreCase(location).stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getJobsByDateRange(Long startDate, Long endDate) {
        return jobRepository.findByPostedDateBetween(startDate, endDate).stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllLaborFunctions() {
        return jobRepository.findAllLaborFunctions();
    }

    @Override
    public List<String> getAllLocations() {
        return jobRepository.findAllLocations();
    }

    @Override
    public long getJobCountByFunction(String function) {
        return jobRepository.countByLaborFunction(function);
    }
}