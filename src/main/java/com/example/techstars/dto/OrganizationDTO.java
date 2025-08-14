package com.example.techstars.dto;

import com.example.techstars.model.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private Long id;
    private String title;
    private String url;

    public static OrganizationDTO fromEntity(Organization organization) {
        return OrganizationDTO.builder()
                .id(organization.getId())
                .title(organization.getTitle())
                .url(organization.getUrl())
                .build();
    }
} 