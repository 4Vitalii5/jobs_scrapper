package com.example.techstars.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
    private Long id;
    private String positionName;
    private String jobPageUrl;
    private String logoUrl;
    private String laborFunction;
    private Long postedDate;
    private String description;
    private String location;
    private OrganizationDTO organization;
    private List<TagDTO> tags;
} 