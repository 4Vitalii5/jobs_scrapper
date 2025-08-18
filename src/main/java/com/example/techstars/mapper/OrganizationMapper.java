package com.example.techstars.mapper;

import com.example.techstars.dto.OrganizationDto;
import com.example.techstars.model.Organization;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    OrganizationDto toDto(Organization organization);
}
