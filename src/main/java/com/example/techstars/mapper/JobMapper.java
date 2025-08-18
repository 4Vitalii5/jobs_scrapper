package com.example.techstars.mapper;

import com.example.techstars.dto.JobDto;
import com.example.techstars.model.Job;
import com.example.techstars.model.Location;
import com.example.techstars.model.Tag;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrganizationMapper.class)
public interface JobMapper {
    JobDto toDto(Job job);

    default Set<String> toStringSet(Set<Tag> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }

    default Set<String> locationsToStrings(Set<Location> locations) {
        if (locations == null) {
            return Collections.emptySet();
        }
        return locations.stream().map(Location::getName).collect(Collectors.toSet());
    }
}