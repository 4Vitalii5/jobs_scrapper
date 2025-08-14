package com.example.techstars.mapper;

import com.example.techstars.dto.JobDto;
import com.example.techstars.model.Job;
import com.example.techstars.model.Tag;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrganizationMapper.class)
public interface JobMapper {
    JobDto toDto(Job job);

    default Set<String> toStringSet(Set<Tag> tags) {
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }
}